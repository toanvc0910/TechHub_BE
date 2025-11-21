package com.techhub.app.aiservice.service;

import com.techhub.app.aiservice.config.ChatbotProperties;
import com.techhub.app.aiservice.config.OpenAiProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Service to generate embeddings using OpenAI Embedding API
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class EmbeddingService {

    private final RestTemplate restTemplate;
    private final OpenAiProperties openAiProperties;
    private final ChatbotProperties chatbotProperties;

    private static final int EMBEDDING_DIMENSION = 1536;

    /**
     * Generate embedding vector from text
     * 
     * @param text Input text to embed
     * @return List of doubles representing the embedding vector
     */
    public List<Double> generateEmbedding(String text) {
        if (text == null || text.trim().isEmpty()) {
            log.warn("⚠️ Cannot generate embedding for empty text");
            return Collections.emptyList();
        }

        if (chatbotProperties.isMockEmbeddings()) {
            log.warn("🧠 Mock embeddings enabled, returning dummy embedding");
            return generateDummyEmbedding();
        }

        String url = openAiProperties.getBaseUrl() + "/embeddings";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(getApiKey());

        Map<String, Object> body = new HashMap<>();
        body.put("model", chatbotProperties.getEmbedding().getModelId());
        body.put("input", text);

        try {
            ResponseEntity<Map> response = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    new HttpEntity<>(body, headers),
                    Map.class);

            Map<String, Object> responseBody = response.getBody();
            if (responseBody != null && responseBody.containsKey("data")) {
                List<Map<String, Object>> data = (List<Map<String, Object>>) responseBody.get("data");
                if (!data.isEmpty()) {
                    List<Double> embedding = (List<Double>) data.get(0).get("embedding");
                    log.debug("✅ Generated embedding with {} dimensions", embedding.size());
                    return embedding;
                }
            }

            log.error("❌ Invalid response from OpenAI Embedding API");
            return generateDummyEmbedding();

        } catch (Exception e) {
            log.error("❌ Failed to generate embedding from OpenAI", e);
            return generateDummyEmbedding();
        }
    }

    /**
     * Generate embeddings for multiple texts in batch
     */
    public List<List<Double>> generateEmbeddingsBatch(List<String> texts) {
        if (texts == null || texts.isEmpty()) {
            return Collections.emptyList();
        }

        if (chatbotProperties.isMockEmbeddings()) {
            log.warn("🧠 Mock embeddings enabled, returning dummy embeddings");
            return texts.stream()
                    .map(t -> generateDummyEmbedding())
                    .collect(Collectors.toList());
        }

        String url = openAiProperties.getBaseUrl() + "/embeddings";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(getApiKey());

        Map<String, Object> body = new HashMap<>();
        body.put("model", chatbotProperties.getEmbedding().getModelId());
        body.put("input", texts);

        try {
            ResponseEntity<Map> response = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    new HttpEntity<>(body, headers),
                    Map.class);

            Map<String, Object> responseBody = response.getBody();
            if (responseBody != null && responseBody.containsKey("data")) {
                List<Map<String, Object>> data = (List<Map<String, Object>>) responseBody.get("data");
                List<List<Double>> embeddings = data.stream()
                        .map(item -> (List<Double>) item.get("embedding"))
                        .collect(Collectors.toList());
                log.info("✅ Generated {} embeddings", embeddings.size());
                return embeddings;
            }

            log.error("❌ Invalid response from OpenAI Embedding API");
            return texts.stream().map(t -> generateDummyEmbedding()).collect(Collectors.toList());

        } catch (Exception e) {
            log.error("❌ Failed to generate batch embeddings from OpenAI", e);
            return texts.stream().map(t -> generateDummyEmbedding()).collect(Collectors.toList());
        }
    }

    /**
     * Get embedding dimension for collection creation
     */
    public int getEmbeddingDimension() {
        return EMBEDDING_DIMENSION;
    }

    /**
     * Build text representation for course to embed
     */
    public String buildCourseText(String title, String description, String objectives, String requirements) {
        StringBuilder sb = new StringBuilder();
        sb.append("Course: ").append(title).append("\n");
        if (description != null && !description.isEmpty()) {
            sb.append("Description: ").append(description).append("\n");
        }
        if (objectives != null && !objectives.isEmpty()) {
            sb.append("Objectives: ").append(objectives).append("\n");
        }
        if (requirements != null && !requirements.isEmpty()) {
            sb.append("Requirements: ").append(requirements);
        }
        return sb.toString();
    }

    /**
     * Build text representation for user profile to embed
     */
    public String buildUserProfileText(String userId, String skills, String interests, String completedCourses) {
        StringBuilder sb = new StringBuilder();
        sb.append("User ID: ").append(userId).append("\n");
        if (skills != null && !skills.isEmpty()) {
            sb.append("Skills: ").append(skills).append("\n");
        }
        if (interests != null && !interests.isEmpty()) {
            sb.append("Interests: ").append(interests).append("\n");
        }
        if (completedCourses != null && !completedCourses.isEmpty()) {
            sb.append("Completed: ").append(completedCourses);
        }
        return sb.toString();
    }

    private String getApiKey() {
        return openAiProperties.getApiKey();
    }

    /**
     * Generate dummy embedding for testing when OpenAI is disabled
     */
    private List<Double> generateDummyEmbedding() {
        Random random = new Random();
        List<Double> dummy = new ArrayList<>(EMBEDDING_DIMENSION);
        for (int i = 0; i < EMBEDDING_DIMENSION; i++) {
            dummy.add(random.nextDouble() * 2 - 1); // Random values between -1 and 1
        }
        return dummy;
    }
}
