package com.techhub.app.aiservice.client;

import com.techhub.app.aiservice.config.QdrantProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.*;

/**
 * REST Client for Qdrant Vector Database
 * API Docs: https://qdrant.tech/documentation/
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class QdrantClient {

    private final RestTemplate restTemplate;
    private final QdrantProperties qdrantProperties;

    /**
     * Upsert (insert/update) vectors into Qdrant collection
     */
    public void upsertPoints(String collectionName, List<QdrantPoint> points) {
        String url = qdrantProperties.getHost() + "/collections/" + collectionName + "/points";

        Map<String, Object> body = new HashMap<>();
        body.put("points", points);

        try {
            restTemplate.exchange(
                    url,
                    HttpMethod.PUT,
                    new HttpEntity<>(body, buildHeaders()),
                    Map.class);
            log.info("✅ Upserted {} points to collection: {}", points.size(), collectionName);
        } catch (Exception e) {
            log.error("❌ Failed to upsert points to Qdrant collection: {}", collectionName, e);
            throw new RuntimeException("Qdrant upsert failed", e);
        }
    }

    /**
     * Search similar vectors in Qdrant
     */
    public List<Map<String, Object>> searchSimilar(String collectionName, List<Double> queryVector, int limit) {
        String url = qdrantProperties.getHost() + "/collections/" + collectionName + "/points/search";

        Map<String, Object> body = new HashMap<>();
        body.put("vector", queryVector);
        body.put("limit", limit);
        body.put("with_payload", true);
        body.put("with_vector", false);

        try {
            ResponseEntity<Map> response = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    new HttpEntity<>(body, buildHeaders()),
                    Map.class);

            Map<String, Object> responseBody = response.getBody();
            if (responseBody != null && responseBody.containsKey("result")) {
                return (List<Map<String, Object>>) responseBody.get("result");
            }
            return Collections.emptyList();
        } catch (Exception e) {
            log.error("❌ Failed to search Qdrant collection: {}", collectionName, e);
            return Collections.emptyList();
        }
    }

    /**
     * Delete point by ID
     */
    public void deletePoint(String collectionName, String pointId) {
        String url = qdrantProperties.getHost() + "/collections/" + collectionName + "/points/delete";

        Map<String, Object> body = new HashMap<>();
        body.put("points", Collections.singletonList(pointId));

        try {
            restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    new HttpEntity<>(body, buildHeaders()),
                    Map.class);
            log.info("🗑️ Deleted point {} from collection: {}", pointId, collectionName);
        } catch (Exception e) {
            log.error("❌ Failed to delete point from Qdrant", e);
        }
    }

    /**
     * Clear all points in a collection (delete and recreate)
     * Use this before full reindex to remove stale/orphaned embeddings
     */
    public void clearCollection(String collectionName, int vectorSize) {
        String url = qdrantProperties.getHost() + "/collections/" + collectionName;

        try {
            // Delete the collection
            restTemplate.exchange(
                    url,
                    HttpMethod.DELETE,
                    new HttpEntity<>(buildHeaders()),
                    Map.class);
            log.info("🗑️ Deleted collection: {}", collectionName);

            // Recreate the collection
            Map<String, Object> body = new HashMap<>();
            Map<String, Object> vectorsConfig = new HashMap<>();
            vectorsConfig.put("size", vectorSize);
            vectorsConfig.put("distance", "Cosine");
            body.put("vectors", vectorsConfig);

            restTemplate.exchange(
                    url,
                    HttpMethod.PUT,
                    new HttpEntity<>(body, buildHeaders()),
                    Map.class);
            log.info("✅ Recreated collection: {}", collectionName);
        } catch (Exception e) {
            log.error("❌ Failed to clear collection: {}", collectionName, e);
            throw new RuntimeException("Failed to clear collection: " + collectionName, e);
        }
    }

    /**
     * Check if Qdrant is healthy by trying to list collections
     * This is more reliable than a health endpoint as it verifies actual connectivity
     */
    public boolean checkHealth() {
        String url = qdrantProperties.getHost() + "/collections";
        try {
            ResponseEntity<Map> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    new HttpEntity<>(buildHeaders()),
                    Map.class);
            return response.getStatusCode().is2xxSuccessful();
        } catch (Exception e) {
            log.warn("⚠️ Qdrant health check failed: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Get Qdrant version information
     */
    public String getVersion() {
        String url = qdrantProperties.getHost() + "/";
        try {
            ResponseEntity<Map> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    new HttpEntity<>(buildHeaders()),
                    Map.class);
            Map<String, Object> body = response.getBody();
            if (body != null) {
                // Qdrant root endpoint returns version in different possible formats
                if (body.containsKey("version")) {
                    Object version = body.get("version");
                    return version != null ? version.toString() : null;
                }
                // Sometimes version is nested in result
                if (body.containsKey("result")) {
                    Map<String, Object> result = (Map<String, Object>) body.get("result");
                    if (result != null && result.containsKey("version")) {
                        Object version = result.get("version");
                        return version != null ? version.toString() : null;
                    }
                }
            }
            return null;
        } catch (Exception e) {
            log.warn("⚠️ Failed to get Qdrant version: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Get collection info including point count
     */
    public Map<String, Object> getCollectionInfo(String collectionName) {
        String url = qdrantProperties.getHost() + "/collections/" + collectionName;

        try {
            ResponseEntity<Map> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    new HttpEntity<>(buildHeaders()),
                    Map.class);

            Map<String, Object> body = response.getBody();
            if (body != null && body.containsKey("result")) {
                return (Map<String, Object>) body.get("result");
            }
            return Collections.emptyMap();
        } catch (Exception e) {
            log.error("❌ Failed to get collection info: {}", collectionName, e);
            return Collections.emptyMap();
        }
    }

    /**
     * Retrieve a single point by ID
     */
    public Map<String, Object> retrievePoint(String collectionName, String pointId) {
        String url = qdrantProperties.getHost() + "/collections/" + collectionName + "/points/" + pointId;

        try {
            ResponseEntity<Map> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    new HttpEntity<>(buildHeaders()),
                    Map.class);

            Map<String, Object> body = response.getBody();
            if (body != null && body.containsKey("result")) {
                return (Map<String, Object>) body.get("result");
            }
            return null;
        } catch (Exception e) {
            log.warn("⚠️ Failed to retrieve point {} from collection {}: {}", pointId, collectionName, e.getMessage());
            return null;
        }
    }

    /**
     * Create collection if not exists
     */
    public void createCollectionIfNotExists(String collectionName, int vectorSize) {
        String url = qdrantProperties.getHost() + "/collections/" + collectionName;

        try {
            // Check if collection exists
            ResponseEntity<Map> checkResponse = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    new HttpEntity<>(buildHeaders()),
                    Map.class);
            log.info("✅ Collection {} already exists", collectionName);
        } catch (Exception e) {
            // Collection doesn't exist, create it
            log.info("📦 Creating collection: {}", collectionName);

            Map<String, Object> body = new HashMap<>();
            Map<String, Object> vectorsConfig = new HashMap<>();
            vectorsConfig.put("size", vectorSize);
            vectorsConfig.put("distance", "Cosine");
            body.put("vectors", vectorsConfig);

            try {
                restTemplate.exchange(
                        url,
                        HttpMethod.PUT,
                        new HttpEntity<>(body, buildHeaders()),
                        Map.class);
                log.info("✅ Collection created: {}", collectionName);
            } catch (Exception createEx) {
                log.error("❌ Failed to create Qdrant collection: {}", collectionName, createEx);
            }
        }
    }

    private HttpHeaders buildHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        String apiKey = qdrantProperties.getApiKey();
        if (apiKey != null && !apiKey.isEmpty()) {
            headers.set("api-key", apiKey);
        }

        return headers;
    }

    /**
     * DTO for Qdrant Point
     */
    public static class QdrantPoint {
        private String id;
        private List<Double> vector;
        private Map<String, Object> payload;

        public QdrantPoint(String id, List<Double> vector, Map<String, Object> payload) {
            this.id = id;
            this.vector = vector;
            this.payload = payload;
        }

        public String getId() {
            return id;
        }

        public List<Double> getVector() {
            return vector;
        }

        public Map<String, Object> getPayload() {
            return payload;
        }
    }
}
