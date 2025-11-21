package com.techhub.app.proxyclient.controller;

import com.techhub.app.proxyclient.client.AiServiceClient;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/proxy/ai")
@RequiredArgsConstructor
public class AiProxyController {

    private final AiServiceClient aiServiceClient;

    @PostMapping("/exercises/generate")
    public ResponseEntity<String> generateExercises(@RequestBody Object request,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        return aiServiceClient.generateExercises(request, authHeader);
    }

    @PostMapping("/learning-paths/generate")
    public ResponseEntity<String> generateLearningPaths(@RequestBody Object request,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        return aiServiceClient.generateLearningPath(request, authHeader);
    }

    @PostMapping("/recommendations/realtime")
    public ResponseEntity<String> recommendRealtime(@RequestBody Object request,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        return aiServiceClient.recommendRealtime(request, authHeader);
    }

    @PostMapping("/recommendations/scheduled")
    public ResponseEntity<String> recommendScheduled(@RequestBody Object request,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        return aiServiceClient.recommendScheduled(request, authHeader);
    }

    @PostMapping("/chat/messages")
    public ResponseEntity<String> chat(@RequestBody Object request,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        return aiServiceClient.chat(request, authHeader);
    }

    @PostMapping("/admin/reindex-courses")
    public ResponseEntity<String> reindexCourses(
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        return aiServiceClient.reindexCourses(authHeader);
    }

    @PostMapping("/admin/qdrant-stats")
    public ResponseEntity<String> getQdrantStats(
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        return aiServiceClient.getQdrantStats(authHeader);
    }
}
