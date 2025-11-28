package com.techhub.app.aiservice.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class OpenAiGateway {

    private final RestTemplate restTemplate;
    private final WebClient webClient;
    private final OpenAiProperties openAiProperties;
    private final ChatbotProperties chatbotProperties;
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Generate structured JSON response (blocking - original method)
     */
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

    /**
     * Generate streaming response from OpenAI (reactive - new method)
     * Returns a Flux of text chunks as they arrive from OpenAI
     */
    public Flux<String> generateStreamingResponse(String prompt, String systemPrompt) {
        if (chatbotProperties.isMockEmbeddings()) {
            log.info("🧠 Mock streaming mode enabled, returning stubbed stream");
            return createMockStream(prompt);
        }

        Map<String, Object> body = new HashMap<>();
        body.put("model", openAiProperties.getChat().getModel());
        body.put("messages", List.of(
                Map.of("role", "system", "content",
                        systemPrompt != null ? systemPrompt : chatbotProperties.getSystemPrompt()),
                Map.of("role", "user", "content", prompt)));
        body.put("temperature", 0.3);
        body.put("max_tokens", openAiProperties.getChat().getMaxOutputTokens());
        body.put("stream", true); // Enable streaming

        String requestBody;
        try {
            requestBody = objectMapper.writeValueAsString(body);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize request body: {}", e.getMessage());
            return Flux.error(e);
        }

        return webClient.post()
                .uri(openAiProperties.getBaseUrl() + "/chat/completions")
                .header("Authorization", "Bearer " + openAiProperties.getApiKey())
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(requestBody)
                .retrieve()
                .bodyToFlux(String.class)
                .filter(line -> !line.isBlank() && !line.equals("[DONE]"))
                .map(this::extractContentFromStreamChunk)
                .filter(content -> content != null && !content.isEmpty())
                .doOnError(error -> log.error("Streaming error: {}", error.getMessage()))
                .onErrorResume(error -> Flux.just("[ERROR] " + error.getMessage()));
    }

    /**
     * Generate streaming response with conversation history
     */
    public Flux<String> generateStreamingResponseWithHistory(List<Map<String, String>> messages) {
        log.info("🧠 [OpenAiGateway] ===== GENERATING STREAMING RESPONSE =====");
        log.info("🧠 [OpenAiGateway] Messages count: {}", messages.size());
        log.info("🧠 [OpenAiGateway] Mock mode: {}", chatbotProperties.isMockEmbeddings());

        if (chatbotProperties.isMockEmbeddings()) {
            log.info("🧠 [OpenAiGateway] Using mock streaming mode");
            String lastUserMessage = messages.stream()
                    .filter(m -> "user".equals(m.get("role")))
                    .reduce((first, second) -> second)
                    .map(m -> m.get("content"))
                    .orElse("Hello");
            log.info("🧠 [OpenAiGateway] Mock last user message: {}", lastUserMessage);
            return createMockStream(lastUserMessage);
        }

        Map<String, Object> body = new HashMap<>();
        body.put("model", openAiProperties.getChat().getModel());
        body.put("messages", messages);
        body.put("temperature", 0.3);
        body.put("max_tokens", openAiProperties.getChat().getMaxOutputTokens());
        body.put("stream", true);

        String requestBody;
        try {
            requestBody = objectMapper.writeValueAsString(body);
            log.info("🧠 [OpenAiGateway] Request to OpenAI: {}",
                    requestBody.substring(0, Math.min(500, requestBody.length())));
        } catch (JsonProcessingException e) {
            log.error("🧠 [OpenAiGateway] Failed to serialize request body: {}", e.getMessage());
            return Flux.error(e);
        }

        log.info("🧠 [OpenAiGateway] Calling OpenAI API at: {}", openAiProperties.getBaseUrl() + "/chat/completions");
        return webClient.post()
                .uri(openAiProperties.getBaseUrl() + "/chat/completions")
                .header("Authorization", "Bearer " + openAiProperties.getApiKey())
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(requestBody)
                .retrieve()
                .bodyToFlux(String.class)
                .doOnNext(rawLine -> log.debug("🧠 [OpenAiGateway] Raw line from OpenAI: {}", rawLine))
                .filter(line -> !line.isBlank() && !line.equals("[DONE]"))
                .map(chunk -> {
                    String content = extractContentFromStreamChunk(chunk);
                    log.info("🧠 [OpenAiGateway] Extracted content: {}", content);
                    return content;
                })
                .filter(content -> content != null && !content.isEmpty())
                .doOnComplete(() -> log.info("🧠 [OpenAiGateway] ===== OPENAI STREAM COMPLETED ====="))
                .doOnError(error -> log.error("🧠 [OpenAiGateway] Streaming error: {}", error.getMessage(), error))
                .onErrorResume(error -> Flux.just("[ERROR] " + error.getMessage()));
    }

    /**
     * Extract content from OpenAI SSE stream chunk
     * OpenAI sends data in format: data:
     * {"id":"...","choices":[{"delta":{"content":"token"}}]}
     */
    private String extractContentFromStreamChunk(String chunk) {
        try {
            // Handle SSE format - OpenAI sends "data: {...}"
            String jsonPart = chunk;
            if (chunk.startsWith("data: ")) {
                jsonPart = chunk.substring(6).trim();
            }

            if (jsonPart.isEmpty() || jsonPart.equals("[DONE]")) {
                return "";
            }

            JsonNode root = objectMapper.readTree(jsonPart);
            JsonNode choices = root.get("choices");
            if (choices != null && choices.isArray() && choices.size() > 0) {
                JsonNode delta = choices.get(0).get("delta");
                if (delta != null && delta.has("content")) {
                    return delta.get("content").asText();
                }
            }
            return "";
        } catch (Exception e) {
            log.trace("Failed to parse stream chunk: {} - {}", chunk, e.getMessage());
            return "";
        }
    }

    /**
     * Create mock stream for testing without OpenAI API
     */
    private Flux<String> createMockStream(String prompt) {
        String mockResponse = "Đây là phản hồi mẫu cho câu hỏi: \"" + prompt + "\". " +
                "Tính năng streaming đang hoạt động! " +
                "Mỗi từ sẽ được gửi từng phần một. " +
                "Điều này mô phỏng cách OpenAI API streaming hoạt động.";

        String[] words = mockResponse.split(" ");
        return Flux.fromArray(words)
                .delayElements(java.time.Duration.ofMillis(100))
                .map(word -> word + " ");
    }

    private Map<String, Object> stubbedResponse(String prompt, Object contextPayload) {
        Map<String, Object> response = new HashMap<>();
        response.put("provider", "stub");
        response.put("prompt_echo", prompt);
        response.put("context", contextPayload);
        response.put("generated_at", Instant.now().toString());
        return response;
    }
}
