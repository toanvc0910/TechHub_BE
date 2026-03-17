package com.techhub.app.aiservice.service;

import com.techhub.app.aiservice.config.ChatbotProperties;
import com.techhub.app.aiservice.config.GeminiProperties;
import com.techhub.app.aiservice.config.OpenAiProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Service to generate embeddings using Gemini Embedding API
 * (text-embedding-004).
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class EmbeddingService {

    private final RestTemplate restTemplate;
    private final GeminiProperties geminiProperties;
    private final OpenAiProperties openAiProperties;
    private final ChatbotProperties chatbotProperties;
    private final AiProviderConfigService aiProviderConfigService;

    private static final int GEMINI_EMBEDDING_DIMENSION = 768;
    private static final int OPENAI_EMBEDDING_DIMENSION = 1536;

    public List<Double> generateEmbedding(String text) {
        if (text == null || text.trim().isEmpty()) {
            log.warn("Cannot generate embedding for empty text");
            return Collections.emptyList();
        }

        if (chatbotProperties.isMockEmbeddings()) {
            log.warn("Mock embeddings enabled, returning dummy embedding");
            return generateDummyEmbedding();
        }

        String provider = currentProvider();

        try {
            List<Double> embedding = "openai".equals(provider)
                    ? generateOpenAiEmbedding(text)
                    : generateGeminiEmbedding(text);

            if (embedding != null && !embedding.isEmpty()) {
                log.debug("Generated embedding with {} dimensions via {}", embedding.size(), provider);
                return embedding;
            }

            log.error("Invalid response from {} Embedding API", provider);
            return generateDummyEmbedding();
        } catch (Exception e) {
            log.error("Failed to generate embedding from {}", provider, e);
            return generateDummyEmbedding();
        }
    }

    public List<List<Double>> generateEmbeddingsBatch(List<String> texts) {
        if (texts == null || texts.isEmpty()) {
            return Collections.emptyList();
        }

        if (chatbotProperties.isMockEmbeddings()) {
            log.warn("Mock embeddings enabled, returning dummy embeddings");
            return texts.stream().map(t -> generateDummyEmbedding()).collect(Collectors.toList());
        }

        String provider = currentProvider();

        try {
            List<List<Double>> embeddings;
            if ("openai".equals(provider)) {
                embeddings = generateOpenAiEmbeddingsBatch(texts);
            } else {
                embeddings = generateGeminiEmbeddingsBatch(texts);
            }

            if (embeddings != null && !embeddings.isEmpty()) {
                log.info("Generated {} embeddings", embeddings.size());
                return embeddings;
            }

            log.error("Invalid response from {} Embedding API", provider);
            return texts.stream().map(t -> generateDummyEmbedding()).collect(Collectors.toList());
        } catch (Exception e) {
            log.error("Failed to generate batch embeddings from {}", provider, e);
            return texts.stream().map(t -> generateDummyEmbedding()).collect(Collectors.toList());
        }
    }

    public int getEmbeddingDimension() {
        return "openai".equals(currentProvider()) ? OPENAI_EMBEDDING_DIMENSION : GEMINI_EMBEDDING_DIMENSION;
    }

    @SuppressWarnings("unchecked")
    private List<Double> generateGeminiEmbedding(String text) {
        String model = resolveGeminiEmbeddingModel();
        String url = geminiProperties.getBaseUrl() + "/models/" + model + ":embedContent?key="
                + geminiProperties.getApiKey();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        Map<String, Object> body = new HashMap<>();
        body.put("content", Map.of("parts", List.of(Map.of("text", text))));

        ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.POST, new HttpEntity<>(body, headers),
                Map.class);
        Map<String, Object> responseBody = response.getBody();
        if (responseBody != null && responseBody.containsKey("embedding")) {
            Map<String, Object> embObj = (Map<String, Object>) responseBody.get("embedding");
            return (List<Double>) embObj.get("values");
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    private List<List<Double>> generateGeminiEmbeddingsBatch(List<String> texts) {
        String model = resolveGeminiEmbeddingModel();
        String url = geminiProperties.getBaseUrl() + "/models/" + model + ":batchEmbedContents?key="
                + geminiProperties.getApiKey();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        List<Map<String, Object>> requests = texts.stream()
                .map(t -> Map.<String, Object>of("content", Map.of("parts", List.of(Map.of("text", t)))))
                .collect(Collectors.toList());
        Map<String, Object> body = new HashMap<>();
        body.put("requests", requests);

        ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.POST, new HttpEntity<>(body, headers),
                Map.class);
        Map<String, Object> responseBody = response.getBody();
        if (responseBody != null && responseBody.containsKey("embeddings")) {
            List<Map<String, Object>> embList = (List<Map<String, Object>>) responseBody.get("embeddings");
            return embList.stream().map(e -> (List<Double>) e.get("values")).collect(Collectors.toList());
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    private List<Double> generateOpenAiEmbedding(String text) {
        String model = resolveOpenAiEmbeddingModel();
        String url = openAiProperties.getBaseUrl() + "/embeddings";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(openAiProperties.getApiKey());

        Map<String, Object> body = new HashMap<>();
        body.put("model", model);
        body.put("input", text);

        ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.POST, new HttpEntity<>(body, headers),
                Map.class);
        Map<String, Object> responseBody = response.getBody();
        if (responseBody != null && responseBody.containsKey("data")) {
            List<Map<String, Object>> data = (List<Map<String, Object>>) responseBody.get("data");
            if (!data.isEmpty()) {
                return (List<Double>) data.get(0).get("embedding");
            }
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    private List<List<Double>> generateOpenAiEmbeddingsBatch(List<String> texts) {
        String model = resolveOpenAiEmbeddingModel();
        String url = openAiProperties.getBaseUrl() + "/embeddings";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(openAiProperties.getApiKey());

        Map<String, Object> body = new HashMap<>();
        body.put("model", model);
        body.put("input", texts);

        ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.POST, new HttpEntity<>(body, headers),
                Map.class);
        Map<String, Object> responseBody = response.getBody();
        if (responseBody != null && responseBody.containsKey("data")) {
            List<Map<String, Object>> data = (List<Map<String, Object>>) responseBody.get("data");
            return data.stream().map(item -> (List<Double>) item.get("embedding")).collect(Collectors.toList());
        }
        return null;
    }

    private String currentProvider() {
        return aiProviderConfigService.getProvider();
    }

    private String resolveOpenAiEmbeddingModel() {
        String explicitModel = chatbotProperties.getEmbedding().getModelId();
        if (explicitModel != null && !explicitModel.isBlank()) {
            return explicitModel;
        }
        String configured = chatbotProperties.getEmbedding().getOpenaiModelId();
        return (configured == null || configured.isBlank()) ? "text-embedding-3-small" : configured;
    }

    private String resolveGeminiEmbeddingModel() {
        String explicitModel = chatbotProperties.getEmbedding().getModelId();
        if (explicitModel != null && !explicitModel.isBlank()) {
            return explicitModel;
        }
        String configured = chatbotProperties.getEmbedding().getGeminiModelId();
        return (configured == null || configured.isBlank()) ? "text-embedding-004" : configured;
    }

    public String buildCourseText(String title, String description, String objectives, String requirements) {
        StringBuilder sb = new StringBuilder();
        sb.append("Course: ").append(title).append("\n");
        if (description != null && !description.isEmpty())
            sb.append("Description: ").append(description).append("\n");
        if (objectives != null && !objectives.isEmpty())
            sb.append("Objectives: ").append(objectives).append("\n");
        if (requirements != null && !requirements.isEmpty())
            sb.append("Requirements: ").append(requirements);
        return sb.toString();
    }

    public String buildUserProfileText(String userId, String skills, String interests, String completedCourses) {
        StringBuilder sb = new StringBuilder();
        sb.append("User ID: ").append(userId).append("\n");
        if (skills != null && !skills.isEmpty())
            sb.append("Skills: ").append(skills).append("\n");
        if (interests != null && !interests.isEmpty())
            sb.append("Interests: ").append(interests).append("\n");
        if (completedCourses != null && !completedCourses.isEmpty())
            sb.append("Completed: ").append(completedCourses);
        return sb.toString();
    }

    private List<Double> generateDummyEmbedding() {
        Random random = new Random();
        int dim = getEmbeddingDimension();
        List<Double> dummy = new ArrayList<>(dim);
        for (int i = 0; i < dim; i++) {
            dummy.add(random.nextDouble() * 2 - 1);
        }
        return dummy;
    }
}
