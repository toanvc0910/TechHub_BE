package com.techhub.app.proxyclient.controller;

import com.techhub.app.proxyclient.client.LearningPathServiceClient;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/proxy/learning-paths")
@RequiredArgsConstructor
public class LearningPathProxyController {

    private final LearningPathServiceClient learningPathServiceClient;

    @GetMapping
    public ResponseEntity<String> getAllLearningPaths(@RequestParam(defaultValue = "0") int page,
                                                     @RequestParam(defaultValue = "10") int size,
                                                     @RequestParam(required = false) String search) {
        return learningPathServiceClient.getAllLearningPaths(page, size, search);
    }

    @PostMapping
    public ResponseEntity<String> createLearningPath(@RequestBody Object createRequest,
                                                   @RequestHeader("Authorization") String authHeader) {
        return learningPathServiceClient.createLearningPath(createRequest, authHeader);
    }

    @GetMapping("/{pathId}")
    public ResponseEntity<String> getLearningPathById(@PathVariable String pathId) {
        return learningPathServiceClient.getLearningPathById(pathId);
    }

    @PutMapping("/{pathId}")
    public ResponseEntity<String> updateLearningPath(@PathVariable String pathId,
                                                   @RequestBody Object updateRequest,
                                                   @RequestHeader("Authorization") String authHeader) {
        return learningPathServiceClient.updateLearningPath(pathId, updateRequest, authHeader);
    }

    @DeleteMapping("/{pathId}")
    public ResponseEntity<String> deleteLearningPath(@PathVariable String pathId,
                                                   @RequestHeader("Authorization") String authHeader) {
        return learningPathServiceClient.deleteLearningPath(pathId, authHeader);
    }

    @PostMapping("/{pathId}/enroll")
    public ResponseEntity<String> enrollLearningPath(@PathVariable String pathId,
                                                   @RequestHeader("Authorization") String authHeader) {
        return learningPathServiceClient.enrollLearningPath(pathId, authHeader);
    }

    @GetMapping("/{pathId}/progress")
    public ResponseEntity<String> getPathProgress(@PathVariable String pathId,
                                                @RequestHeader("Authorization") String authHeader) {
        return learningPathServiceClient.getPathProgress(pathId, authHeader);
    }
}
