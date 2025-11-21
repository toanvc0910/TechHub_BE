package com.techhub.app.aiservice.service;

import com.techhub.app.aiservice.config.ChatbotProperties;
import com.techhub.app.aiservice.config.OpenAiProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class OpenAiGateway {

    private final RestTemplate restTemplate;
    private final OpenAiProperties openAiProperties;
    private final ChatbotProperties chatbotProperties;

    public Object generateStructuredJson(String prompt, Object contextPayload) {
        if (chatbotProperties.isMockEmbeddings()) {
            log.info("🧠 Mock mode enabled, returning stubbed response");
            return stubbedResponse(prompt, contextPayload);
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(openAiProperties.getApiKey());
        headers.setContentType(MediaType.APPLICATION_JSON);

        Map<String, Object> body = new HashMap<>();
        body.put("model", openAiProperties.getChat().getModel());
        body.put("messages", List.of(
                Map.of("role", "system", "content", chatbotProperties.getSystemPrompt()),
                Map.of("role", "user", "content", prompt)));
        body.put("temperature", 0.3);
        body.put("max_tokens", openAiProperties.getChat().getMaxOutputTokens());
        body.put("response_format", Map.of("type", "json_object"));

        try {
            ResponseEntity<Map> response = restTemplate.postForEntity(
                    openAiProperties.getBaseUrl() + "/chat/completions",
                    new HttpEntity<>(body, headers),
                    Map.class);
            return response.getBody();
        } catch (Exception ex) {
            log.error("Failed to call OpenAI: {}", ex.getMessage(), ex);
            return Map.of(
                    "error", ex.getMessage(),
                    "provider", "openai",
                    "fallback", true);
        }
    }

    private Map<String, Object> stubbedResponse(String prompt, Object contextPayload) {
        Map<String, Object> response = new HashMap<>();
        response.put("provider", "stub");
        response.put("prompt_echo", prompt);
        response.put("context", contextPayload); // Can be null
        response.put("generated_at", Instant.now().toString());
        return response;
    }
}
