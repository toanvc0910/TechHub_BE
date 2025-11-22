package com.techhub.app.proxyclient.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;

@FeignClient(name = "AI-SERVICE")
public interface AiServiceClient {

    @PostMapping("/api/ai/exercises/generate")
    ResponseEntity<String> generateExercises(@RequestBody Object request,
            @RequestHeader(value = "Authorization", required = false) String authHeader);

    @PostMapping("/api/ai/learning-paths/generate")
    ResponseEntity<String> generateLearningPath(@RequestBody Object request,
            @RequestHeader(value = "Authorization", required = false) String authHeader);

    @PostMapping("/api/ai/recommendations/realtime")
    ResponseEntity<String> recommendRealtime(@RequestBody Object request,
            @RequestHeader(value = "Authorization", required = false) String authHeader);

    @PostMapping("/api/ai/recommendations/scheduled")
    ResponseEntity<String> recommendScheduled(@RequestBody Object request,
            @RequestHeader(value = "Authorization", required = false) String authHeader);

    @PostMapping("/api/ai/chat/messages")
    ResponseEntity<String> chat(@RequestBody Object request,
            @RequestHeader(value = "Authorization", required = false) String authHeader);

    @PostMapping("/api/ai/admin/reindex-courses")
    ResponseEntity<String> reindexCourses(@RequestHeader(value = "Authorization", required = false) String authHeader);

    @PostMapping("/api/ai/admin/reindex-lessons")
    ResponseEntity<String> reindexLessons(@RequestHeader(value = "Authorization", required = false) String authHeader);

    @PostMapping("/api/ai/admin/reindex-all")
    ResponseEntity<String> reindexAll(@RequestHeader(value = "Authorization", required = false) String authHeader);

    @PostMapping("/api/ai/admin/qdrant-stats")
    ResponseEntity<String> getQdrantStats(@RequestHeader(value = "Authorization", required = false) String authHeader);
}
