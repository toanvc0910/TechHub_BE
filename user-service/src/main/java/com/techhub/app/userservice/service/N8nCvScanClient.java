package com.techhub.app.userservice.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class N8nCvScanClient {

    private final RestTemplate restTemplate = new RestTemplate();
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(15))
            .build();

    @Value("${n8n.webhook-url:}")
    private String webhookUrl;

    @Value("${n8n.callback-base-url:http://localhost:8080}")
    private String callbackBaseUrl;

    @Value("${n8n.callback-secret:dev-secret-change-me}")
    private String callbackSecret;

    @Async
    public void triggerCvScan(UUID applicationId, String cvFileUrl) {
        if (webhookUrl == null || webhookUrl.isBlank()) {
            log.warn("[N8n] webhook-url not configured, skip CV scan. applicationId={}", applicationId);
            return;
        }
        if (cvFileUrl == null || cvFileUrl.isBlank()) {
            log.warn("[N8n] cvFileUrl empty, skip. applicationId={}", applicationId);
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
                log.warn("[N8n] Download CV failed status={} url={}", dlResp.statusCode(), cvFileUrl);
                return;
            }
            byte[] fileBytes = dlResp.body();
            String contentType = dlResp.headers().firstValue("content-type").orElse("application/octet-stream");
            String fileName = extractFileName(cvFileUrl);
            String typeApplication = contentType.contains("pdf") ? "PDF" : "IMG";

            // 2. Build multipart payload
            ByteArrayResource fileResource = new ByteArrayResource(fileBytes) {
                @Override
                public String getFilename() {
                    return fileName;
                }
            };
            MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
            body.add("data", fileResource);
            body.add("applicationId", applicationId.toString());
            body.add("source_system", "TECHHUB");
            body.add("type", "Cv");
            body.add("typeApplication", typeApplication);
            body.add("callbackUrl",
                    callbackBaseUrl + "/api/v1/instructor-applications/n8n-callback");
            body.add("callbackSecret", callbackSecret);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.MULTIPART_FORM_DATA);

            HttpEntity<MultiValueMap<String, Object>> req = new HttpEntity<>(body, headers);
            log.info("[N8n] POSTing multipart to webhook applicationId={} bytes={} typeApplication={}",
                    applicationId, fileBytes.length, typeApplication);
            restTemplate.postForEntity(webhookUrl, req, String.class);
            log.info("[N8n] Webhook accepted applicationId={}", applicationId);
        } catch (Exception ex) {
            log.warn("[N8n] Failed to trigger CV scan applicationId={}: {}", applicationId, ex.getMessage());
        }
    }

    public String getCallbackSecret() {
        return callbackSecret;
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
