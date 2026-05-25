package com.techhub.app.userservice.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.techhub.app.userservice.enums.InstructorApplicationAiStatus;
import com.techhub.app.userservice.repository.InstructorApplicationCertificateRepository;
import com.techhub.app.userservice.repository.InstructorApplicationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class N8nCvScanClient {

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(15))
            .build();
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final InstructorApplicationRepository applicationRepository;
    private final InstructorApplicationCertificateRepository certificateRepository;

    @Value("${n8n.webhook-url:}")
    private String webhookUrl;

    @Async
    public void triggerCvScan(UUID applicationId, String cvFileUrl) {
        Map<String, String> form = new LinkedHashMap<>();
        form.put("applicationId", applicationId.toString());
        form.put("source_system", "E_OFFICE");
        form.put("type", "Cv");
        ScanResult r = doScan(cvFileUrl, form, true);
        if (r.success) {
            updateApp(applicationId, app -> {
                app.setAiStatus(InstructorApplicationAiStatus.PROCESSED);
                app.setAiExtractedData(r.dataJson);
                app.setAiError(null);
            });
        } else {
            updateApp(applicationId, app -> {
                app.setAiStatus(InstructorApplicationAiStatus.FAILED);
                app.setAiError(r.error);
            });
        }
    }

    @Async
    public void triggerCccdScan(UUID applicationId, String fileUrl, boolean front) {
        Map<String, String> form = new LinkedHashMap<>();
        form.put("type", "CCCD");
        form.put("side", front ? "FRONT" : "BACK");
        ScanResult r = doScan(fileUrl, form, false);
        updateApp(applicationId, app -> {
            if (front) {
                app.setCccdFrontStatus(r.success
                        ? InstructorApplicationAiStatus.PROCESSED : InstructorApplicationAiStatus.FAILED);
                app.setCccdFrontData(r.success ? r.dataJson : null);
                app.setCccdFrontError(r.success ? null : r.error);
            } else {
                app.setCccdBackStatus(r.success
                        ? InstructorApplicationAiStatus.PROCESSED : InstructorApplicationAiStatus.FAILED);
                app.setCccdBackData(r.success ? r.dataJson : null);
                app.setCccdBackError(r.success ? null : r.error);
            }
        });
    }

    @Async
    public void triggerCertificateScan(UUID certificateId, String fileUrl) {
        Map<String, String> form = new LinkedHashMap<>();
        form.put("type", "scan-certificate");
        ScanResult r = doScan(fileUrl, form, false);
        updateCert(certificateId, cert -> {
            cert.setAiStatus(r.success
                    ? InstructorApplicationAiStatus.PROCESSED : InstructorApplicationAiStatus.FAILED);
            cert.setAiData(r.success ? r.dataJson : null);
            cert.setAiError(r.success ? null : r.error);
        });
    }

    private ScanResult doScan(String fileUrl, Map<String, String> textParts, boolean includeTypeApplication) {
        String scanType = textParts.getOrDefault("type", "?");
        String scanSide = textParts.getOrDefault("side", "");
        String tag = scanType + (scanSide.isEmpty() ? "" : "/" + scanSide);
        long t0 = System.currentTimeMillis();
        log.info("[N8n][{}] START scan url={}", tag, fileUrl);

        if (webhookUrl == null || webhookUrl.isBlank()) {
            log.warn("[N8n][{}] webhook URL chưa cấu hình", tag);
            return ScanResult.fail("N8n webhook URL chưa cấu hình");
        }
        if (fileUrl == null || fileUrl.isBlank()) {
            log.warn("[N8n][{}] thiếu fileUrl", tag);
            return ScanResult.fail("Thiếu URL file");
        }
        try {
            // Step 1: download file
            long tDl0 = System.currentTimeMillis();
            log.info("[N8n][{}] [1/4] Downloading file...", tag);
            HttpRequest dlReq = HttpRequest.newBuilder()
                    .uri(URI.create(fileUrl))
                    .timeout(Duration.ofSeconds(30))
                    .GET()
                    .build();
            HttpResponse<byte[]> dlResp = httpClient.send(dlReq, HttpResponse.BodyHandlers.ofByteArray());
            long dlMs = System.currentTimeMillis() - tDl0;
            if (dlResp.statusCode() / 100 != 2) {
                log.warn("[N8n][{}] Download FAILED status={} took={}ms", tag, dlResp.statusCode(), dlMs);
                return ScanResult.fail("Tải file từ MinIO thất bại (HTTP " + dlResp.statusCode() + ")");
            }
            byte[] fileBytes = dlResp.body();
            String fileContentType = dlResp.headers().firstValue("content-type").orElse("application/octet-stream");
            String fileName = extractFileName(fileUrl);
            String typeApplication = fileContentType.contains("pdf") ? "PDF" : "IMG";
            log.info("[N8n][{}] [1/4] DONE download bytes={} mime={} name={} took={}ms",
                    tag, fileBytes.length, fileContentType, fileName, dlMs);

            // Step 2: build multipart payload
            long tBuild0 = System.currentTimeMillis();
            String boundary = "----TechHubBoundary" + UUID.randomUUID().toString().replace("-", "");
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            writeFilePart(baos, boundary, "data", fileName, fileContentType, fileBytes);
            for (Map.Entry<String, String> e : textParts.entrySet()) {
                writeTextPart(baos, boundary, e.getKey(), e.getValue());
            }
            writeTextPart(baos, boundary, "typeApplication", typeApplication);
            baos.write(("--" + boundary + "--\r\n").getBytes(StandardCharsets.UTF_8));
            byte[] payload = baos.toByteArray();
            log.info("[N8n][{}] [2/4] Built multipart payload size={}KB took={}ms",
                    tag, payload.length / 1024, System.currentTimeMillis() - tBuild0);

            // Step 3: POST n8n (đây là chỗ thường chậm vì n8n call LLM/OCR)
            long tPost0 = System.currentTimeMillis();
            log.info("[N8n][{}] [3/4] POST -> {} (waiting for n8n+LLM, timeout=120s)", tag, webhookUrl);
            HttpRequest postReq = HttpRequest.newBuilder()
                    .uri(URI.create(webhookUrl))
                    .timeout(Duration.ofSeconds(120))
                    .header("Content-Type", "multipart/form-data; boundary=" + boundary)
                    .header("Accept", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofByteArray(payload))
                    .build();
            HttpResponse<String> resp = httpClient.send(postReq, HttpResponse.BodyHandlers.ofString());
            long postMs = System.currentTimeMillis() - tPost0;
            int bodyLen = resp.body() == null ? 0 : resp.body().length();
            log.info("[N8n][{}] [3/4] DONE n8n response status={} bodyLen={} took={}ms",
                    tag, resp.statusCode(), bodyLen, postMs);

            if (resp.statusCode() / 100 != 2 || resp.body() == null) {
                log.warn("[N8n][{}] n8n non-2xx body={}", tag, resp.body());
                return ScanResult.fail("N8n trả về HTTP " + resp.statusCode()
                        + (resp.body() == null ? "" : ": " + resp.body()));
            }

            // Step 4: parse
            long tParse0 = System.currentTimeMillis();
            JsonNode root = objectMapper.readTree(resp.body());
            JsonNode pl = root.isArray() && root.size() > 0 ? root.get(0) : root;
            int status = pl.path("status").asInt(0);
            long totalMs = System.currentTimeMillis() - t0;
            log.info("[N8n][{}] [4/4] Parsed status={} took={}ms | TOTAL={}ms (dl={}ms post={}ms)",
                    tag, status, System.currentTimeMillis() - tParse0, totalMs, dlMs, postMs);
            if (status == 200 && pl.has("data")) {
                return ScanResult.ok(objectMapper.writeValueAsString(pl.get("data")));
            }
            return ScanResult.fail(pl.path("message").asText("AI trả về status " + status));
        } catch (Exception ex) {
            log.warn("[N8n][{}] Scan FAILED after {}ms: {}", tag, System.currentTimeMillis() - t0, ex.getMessage(), ex);
            return ScanResult.fail("Gửi N8n thất bại: " + ex.getMessage());
        }
    }

    private void updateApp(UUID applicationId,
            java.util.function.Consumer<com.techhub.app.userservice.entity.InstructorApplication> mutator) {
        applicationRepository.findById(applicationId).ifPresent(app -> {
            mutator.accept(app);
            applicationRepository.save(app);
        });
    }

    private void updateCert(UUID certificateId,
            java.util.function.Consumer<com.techhub.app.userservice.entity.InstructorApplicationCertificate> mutator) {
        certificateRepository.findById(certificateId).ifPresent(cert -> {
            mutator.accept(cert);
            certificateRepository.save(cert);
        });
    }

    private void writeFilePart(ByteArrayOutputStream baos, String boundary, String name,
                               String fileName, String contentType, byte[] data) throws Exception {
        baos.write(("--" + boundary + "\r\n").getBytes(StandardCharsets.UTF_8));
        baos.write(("Content-Disposition: form-data; name=\"" + name + "\"; filename=\"" + fileName + "\"\r\n")
                .getBytes(StandardCharsets.UTF_8));
        baos.write(("Content-Type: " + contentType + "\r\n\r\n").getBytes(StandardCharsets.UTF_8));
        baos.write(data);
        baos.write("\r\n".getBytes(StandardCharsets.UTF_8));
    }

    private void writeTextPart(ByteArrayOutputStream baos, String boundary, String name, String value) throws Exception {
        baos.write(("--" + boundary + "\r\n").getBytes(StandardCharsets.UTF_8));
        baos.write(("Content-Disposition: form-data; name=\"" + name + "\"\r\n\r\n")
                .getBytes(StandardCharsets.UTF_8));
        baos.write(value.getBytes(StandardCharsets.UTF_8));
        baos.write("\r\n".getBytes(StandardCharsets.UTF_8));
    }

    private String extractFileName(String url) {
        try {
            String path = URI.create(url).getPath();
            int idx = path.lastIndexOf('/');
            String name = idx >= 0 ? path.substring(idx + 1) : path;
            return name.isBlank() ? "file.bin" : name;
        } catch (Exception e) {
            return "file.bin";
        }
    }

    private static final class ScanResult {
        final boolean success;
        final String dataJson;
        final String error;

        private ScanResult(boolean s, String d, String e) {
            this.success = s;
            this.dataJson = d;
            this.error = e;
        }

        static ScanResult ok(String json) { return new ScanResult(true, json, null); }
        static ScanResult fail(String err) { return new ScanResult(false, null, err); }
    }
}
