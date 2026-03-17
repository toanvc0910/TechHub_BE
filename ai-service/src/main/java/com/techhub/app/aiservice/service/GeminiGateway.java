package com.techhub.app.aiservice.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.techhub.app.aiservice.config.ChatbotProperties;
import com.techhub.app.aiservice.config.GeminiProperties;
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
import java.util.*;
import java.util.stream.Collectors;

/**
 * Gemini AI gateway — implements the shared {@link AiGateway} contract so all
 * existing service callers work unchanged after the field-type swap.
 *
 * <p>
 * Structured-JSON responses are wrapped in the OpenAI {@code choices} shape
 * so downstream parsing code (e.g. {@code choices[0].message.content}) remains
 * identical.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class GeminiGateway implements AiGateway {

    private final RestTemplate restTemplate;
    private final WebClient webClient;
    private final GeminiProperties geminiProperties;
    private final ChatbotProperties chatbotProperties;
    private final AiProviderConfigService aiProviderConfigService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    // -------------------------------------------------------------------------
    // Public API (identical signatures to AiGateway)
    // -------------------------------------------------------------------------

    /**
     * Generate structured JSON response (blocking).
     *
     * <p>
     * Returns an OpenAI-compatible map
     * {@code {"choices":[{"message":{"content":"..."}}]}}
     * so all existing callers that parse {@code choices[0].message.content}
     * continue to work.
     */
    public Object generateStructuredJson(String prompt, Object contextPayload) {
        if (chatbotProperties.isMockEmbeddings()) {
            log.info("🧠 Mock mode enabled, returning stubbed response");
            return stubbedResponse(prompt, contextPayload);
        }

        String model = aiProviderConfigService.getModelForProvider("gemini");
        String url = geminiProperties.getBaseUrl()
                + "/models/" + model
                + ":generateContent?key=" + geminiProperties.getApiKey();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        Map<String, Object> body = new HashMap<>();
        body.put("systemInstruction", Map.of(
                "parts", List.of(Map.of("text", chatbotProperties.getSystemPrompt()))));
        body.put("contents", List.of(Map.of(
                "role", "user",
                "parts", List.of(Map.of("text", prompt)))));
        body.put("generationConfig", Map.of(
                "temperature", 0.3,
                "maxOutputTokens", geminiProperties.getChat().getMaxOutputTokens(),
                "responseMimeType", "application/json"));

        try {
            ResponseEntity<Map> response = restTemplate.postForEntity(
                    url,
                    new HttpEntity<>(body, headers),
                    Map.class);

            Map<String, Object> responseBody = response.getBody();
            if (responseBody == null) {
                return errorResponse("Gemini returned null response");
            }

            String contentText = extractTextFromGeminiResponse(responseBody);
            if (contentText == null) {
                log.error("❌ [GeminiGateway] Could not extract text from response: {}", responseBody.keySet());
                return errorResponse("Could not extract content from Gemini response");
            }

            // Wrap in OpenAI-compatible shape so existing callers need no changes
            return Map.of("choices", List.of(
                    Map.of(
                            "message", Map.of("content", contentText, "role", "assistant"),
                            "finish_reason", "stop")));
        } catch (Exception ex) {
            log.error("❌ [GeminiGateway] Failed to call Gemini: {}", ex.getMessage(), ex);
            return Map.of("error", ex.getMessage(), "provider", "gemini", "fallback", true);
        }
    }

    /**
     * Generate a single-turn streaming response.
     */
    public Flux<String> generateStreamingResponse(String prompt, String systemPrompt) {
        if (chatbotProperties.isMockEmbeddings()) {
            log.info("🧠 Mock streaming mode enabled, returning stubbed stream");
            return createMockStream(prompt);
        }

        String effectiveSystem = systemPrompt != null ? systemPrompt : chatbotProperties.getSystemPrompt();
        List<Map<String, Object>> contents = List.of(
                Map.of("role", "user", "parts", List.of(Map.of("text", prompt))));

        return callGeminiStreaming(buildRequestBody(effectiveSystem, contents));
    }

    /**
     * Generate a streaming response with full conversation history.
     *
     * <p>
     * Converts OpenAI message format to Gemini format:
     * <ul>
     * <li>{@code role:"system"} messages → {@code systemInstruction}</li>
     * <li>{@code role:"assistant"} → {@code role:"model"}</li>
     * </ul>
     */
    public Flux<String> generateStreamingResponseWithHistory(List<Map<String, String>> messages) {
        log.info("🧠 [GeminiGateway] ===== GENERATING STREAMING RESPONSE =====");
        log.info("🧠 [GeminiGateway] Messages count: {}", messages.size());
        log.info("🧠 [GeminiGateway] Mock mode: {}", chatbotProperties.isMockEmbeddings());

        if (chatbotProperties.isMockEmbeddings()) {
            log.info("🧠 [GeminiGateway] Using mock streaming mode");
            String lastUserMessage = messages.stream()
                    .filter(m -> "user".equals(m.get("role")))
                    .reduce((first, second) -> second)
                    .map(m -> m.get("content"))
                    .orElse("Hello");
            log.info("🧠 [GeminiGateway] Mock last user message: {}", lastUserMessage);
            return createMockStream(lastUserMessage);
        }

        // Collect system instructions
        String systemText = messages.stream()
                .filter(m -> "system".equals(m.get("role")))
                .map(m -> m.get("content"))
                .collect(Collectors.joining("\n"));
        if (systemText.isEmpty()) {
            systemText = chatbotProperties.getSystemPrompt();
        }

        // Convert non-system messages; "assistant" → "model" for Gemini
        List<Map<String, Object>> contents = messages.stream()
                .filter(m -> !"system".equals(m.get("role")))
                .map(m -> {
                    String role = "assistant".equals(m.get("role")) ? "model" : m.get("role");
                    return (Map<String, Object>) Map.of(
                            "role", role,
                            "parts", List.of(Map.of("text", m.get("content"))));
                })
                .collect(Collectors.toList());

        Map<String, Object> body = buildRequestBody(systemText, contents);

        String requestBodyStr;
        try {
            requestBodyStr = objectMapper.writeValueAsString(body);
            log.info("🧠 [GeminiGateway] Request to Gemini: {}",
                    requestBodyStr.substring(0, Math.min(500, requestBodyStr.length())));
        } catch (JsonProcessingException e) {
            log.error("🧠 [GeminiGateway] Failed to serialize request body: {}", e.getMessage());
            return Flux.error(e);
        }

        String model = aiProviderConfigService.getModelForProvider("gemini");
        String url = geminiProperties.getBaseUrl()
                + "/models/" + model
                + ":streamGenerateContent?alt=sse&key=" + geminiProperties.getApiKey();
        log.info("🧠 [GeminiGateway] Calling Gemini streaming API");

        return webClient.post()
                .uri(url)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(requestBodyStr)
                .retrieve()
                .bodyToFlux(String.class)
                .doOnNext(rawLine -> log.debug("🧠 [GeminiGateway] Raw line: {}", rawLine))
                .filter(line -> !line.isBlank())
                .map(chunk -> {
                    String content = extractContentFromGeminiStreamChunk(chunk);
                    log.info("🧠 [GeminiGateway] Extracted content: {}", content);
                    return content;
                })
                .filter(content -> content != null && !content.isEmpty())
                .doOnComplete(() -> log.info("🧠 [GeminiGateway] ===== GEMINI STREAM COMPLETED ====="))
                .doOnError(error -> log.error("🧠 [GeminiGateway] Streaming error: {}", error.getMessage(), error))
                .onErrorResume(error -> Flux.just("[ERROR] " + error.getMessage()));
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    private Map<String, Object> buildRequestBody(String systemText, List<Map<String, Object>> contents) {
        Map<String, Object> generationConfig = new HashMap<>();
        generationConfig.put("temperature", 0.3);
        generationConfig.put("maxOutputTokens", geminiProperties.getChat().getMaxOutputTokens());

        Map<String, Object> body = new HashMap<>();
        body.put("systemInstruction", Map.of("parts", List.of(Map.of("text", systemText))));
        body.put("contents", contents);
        body.put("generationConfig", generationConfig);
        return body;
    }

    private Flux<String> callGeminiStreaming(Map<String, Object> body) {
        String requestBody;
        try {
            requestBody = objectMapper.writeValueAsString(body);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize request body: {}", e.getMessage());
            return Flux.error(e);
        }

        String model = aiProviderConfigService.getModelForProvider("gemini");
        String url = geminiProperties.getBaseUrl()
                + "/models/" + model
                + ":streamGenerateContent?alt=sse&key=" + geminiProperties.getApiKey();

        return webClient.post()
                .uri(url)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(requestBody)
                .retrieve()
                .bodyToFlux(String.class)
                .filter(line -> !line.isBlank())
                .map(this::extractContentFromGeminiStreamChunk)
                .filter(content -> content != null && !content.isEmpty())
                .doOnError(error -> log.error("Streaming error: {}", error.getMessage()))
                .onErrorResume(error -> Flux.just("[ERROR] " + error.getMessage()));
    }

    /**
     * Extract the text field from a Gemini {@code generateContent} response body.
     * 
     * <pre>
     * {"candidates":[{"content":{"parts":[{"text":"..."}],"role":"model"}}]}
     * </pre>
     */
    @SuppressWarnings("unchecked")
    private String extractTextFromGeminiResponse(Map<String, Object> response) {
        try {
            List<Map<String, Object>> candidates = (List<Map<String, Object>>) response.get("candidates");
            if (candidates == null || candidates.isEmpty())
                return null;
            Map<String, Object> content = (Map<String, Object>) candidates.get(0).get("content");
            if (content == null)
                return null;
            List<Map<String, Object>> parts = (List<Map<String, Object>>) content.get("parts");
            if (parts == null || parts.isEmpty())
                return null;
            Object text = parts.get(0).get("text");
            return text != null ? text.toString() : null;
        } catch (Exception e) {
            log.error("Failed to extract text from Gemini response: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Extract a text chunk from a Gemini SSE line.
     * 
     * <pre>
     * data: {"candidates":[{"content":{"parts":[{"text":"chunk"}]}}]}
     * </pre>
     */
    private String extractContentFromGeminiStreamChunk(String chunk) {
        try {
            String jsonPart = chunk.startsWith("data: ") ? chunk.substring(6).trim() : chunk;
            if (jsonPart.isEmpty())
                return "";

            JsonNode root = objectMapper.readTree(jsonPart);
            JsonNode candidates = root.get("candidates");
            if (candidates != null && candidates.isArray() && !candidates.isEmpty()) {
                JsonNode content = candidates.get(0).get("content");
                if (content != null) {
                    JsonNode parts = content.get("parts");
                    if (parts != null && parts.isArray() && !parts.isEmpty()) {
                        JsonNode textNode = parts.get(0).get("text");
                        if (textNode != null)
                            return textNode.asText();
                    }
                }
            }
            return "";
        } catch (Exception e) {
            log.trace("Failed to parse Gemini stream chunk: {} — {}", chunk, e.getMessage());
            return "";
        }
    }

    private Flux<String> createMockStream(String prompt) {
        String mockResponse = "Đây là phản hồi mẫu cho câu hỏi: \"" + prompt + "\". "
                + "Tính năng streaming đang hoạt động! "
                + "Mỗi từ sẽ được gửi từng phần một. "
                + "Điều này mô phỏng cách Gemini API streaming hoạt động.";

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

    private Map<String, Object> errorResponse(String message) {
        return Map.of("error", message, "provider", "gemini", "fallback", true);
    }
}
