package com.techhub.app.userservice.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.techhub.app.userservice.enums.InstructorApplicationAiStatus;
import com.techhub.app.userservice.repository.InstructorApplicationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
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

    @Value("${n8n.webhook-url:}")
    private String webhookUrl;

    @Async
    @Transactional
    public void triggerCvScan(UUID applicationId, String cvFileUrl) {
        if (webhookUrl == null || webhookUrl.isBlank()) {
            markFailed(applicationId, "N8n webhook URL chưa cấu hình");
            return;
        }
        if (cvFileUrl == null || cvFileUrl.isBlank()) {
            markFailed(applicationId, "Thiếu URL file CV");
            return;
        }

        try {
            // 1. Download file từ MinIO
            log.info("[N8n] Downloading CV file applicationId={} url={}", applicationId, cvFileUrl);
            HttpRequest dlReq = HttpRequest.newBuilder()
                    .uri(URI.create(cvFileUrl))
                    .timeout(Duration.ofSeconds(30))
                    .GET()
                    .build();
            HttpResponse<byte[]> dlResp = httpClient.send(dlReq, HttpResponse.BodyHandlers.ofByteArray());
            if (dlResp.statusCode() / 100 != 2) {
                markFailed(applicationId, "Tải CV từ MinIO thất bại (HTTP " + dlResp.statusCode() + ")");
                return;
            }
            byte[] fileBytes = dlResp.body();
            String fileContentType = dlResp.headers().firstValue("content-type").orElse("application/octet-stream");
            String fileName = extractFileName(cvFileUrl);
            String typeApplication = fileContentType.contains("pdf") ? "PDF" : "IMG";

            // 2. Build multipart body thủ công — text parts không có Content-Type
            // (giống Postman) để N8n đẩy vào $json.body chứ không phải binary.
            String boundary = "----TechHubBoundary" + UUID.randomUUID().toString().replace("-", "");
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            writeFilePart(baos, boundary, "data", fileName, fileContentType, fileBytes);
            writeTextPart(baos, boundary, "applicationId", applicationId.toString());
            writeTextPart(baos, boundary, "source_system", "E_OFFICE");
            writeTextPart(baos, boundary, "type", "Cv");
            writeTextPart(baos, boundary, "typeApplication", typeApplication);
            baos.write(("--" + boundary + "--\r\n").getBytes(StandardCharsets.UTF_8));
            byte[] payload = baos.toByteArray();

            // 3. POST sync
            log.info("[N8n] POSTing applicationId={} bytes={} typeApplication={}",
                    applicationId, fileBytes.length, typeApplication);
            HttpRequest postReq = HttpRequest.newBuilder()
                    .uri(URI.create(webhookUrl))
                    .timeout(Duration.ofSeconds(120))
                    .header("Content-Type", "multipart/form-data; boundary=" + boundary)
                    .header("Accept", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofByteArray(payload))
                    .build();
            HttpResponse<String> resp = httpClient.send(postReq, HttpResponse.BodyHandlers.ofString());
            log.info("[N8n] Response status={} applicationId={}", resp.statusCode(), applicationId);

            if (resp.statusCode() / 100 != 2 || resp.body() == null) {
                markFailed(applicationId, "N8n trả về HTTP " + resp.statusCode()
                        + (resp.body() == null ? "" : ": " + resp.body()));
                return;
            }

            // 4. Parse JSON response
            JsonNode root = objectMapper.readTree(resp.body());
            JsonNode pl = root.isArray() && root.size() > 0 ? root.get(0) : root;
            int status = pl.path("status").asInt(0);

            if (status == 200 && pl.has("data")) {
                String dataJson = objectMapper.writeValueAsString(pl.get("data"));
                applicationRepository.findById(applicationId).ifPresent(app -> {
                    app.setAiStatus(InstructorApplicationAiStatus.PROCESSED);
                    app.setAiExtractedData(dataJson);
                    app.setAiError(null);
                    applicationRepository.save(app);
                    log.info("[N8n] PROCESSED applicationId={}", applicationId);
                });
            } else {
                String errMsg = pl.path("message").asText("AI trả về status " + status);
                markFailed(applicationId, errMsg);
            }
        } catch (Exception ex) {
            log.warn("[N8n] Failed applicationId={}: {}", applicationId, ex.getMessage(), ex);
            markFailed(applicationId, "Gửi N8n thất bại: " + ex.getMessage());
        }
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

    private void markFailed(UUID applicationId, String error) {
        applicationRepository.findById(applicationId).ifPresent(app -> {
            app.setAiStatus(InstructorApplicationAiStatus.FAILED);
            app.setAiError(error);
            applicationRepository.save(app);
            log.info("[N8n] FAILED applicationId={} error={}", applicationId, error);
        });
    }

    private String extractFileName(String url) {
        try {
            String path = URI.create(url).getPath();
            int idx = path.lastIndexOf('/');
            String name = idx >= 0 ? path.substring(idx + 1) : path;
            return name.isBlank() ? "cv.pdf" : name;
        } catch (Exception e) {
            return "cv.pdf";
        }
    }
}
