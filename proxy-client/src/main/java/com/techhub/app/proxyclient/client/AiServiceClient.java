package com.techhub.app.proxyclient.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.UUID;

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

    @GetMapping("/api/ai/admin/qdrant-stats")
    ResponseEntity<String> getQdrantStats(@RequestHeader(value = "Authorization", required = false) String authHeader);

    // ============================================
    // DRAFT MANAGEMENT ENDPOINTS
    // ============================================

    @GetMapping("/api/ai/drafts/exercises")
    ResponseEntity<String> getExerciseDrafts(
            @RequestParam UUID lessonId,
            @RequestHeader(value = "Authorization", required = false) String authHeader);

    @GetMapping("/api/ai/drafts/exercises/latest")
    ResponseEntity<String> getLatestExerciseDraft(
            @RequestParam UUID lessonId,
            @RequestHeader(value = "Authorization", required = false) String authHeader);

    @GetMapping("/api/ai/drafts/{taskId}")
    ResponseEntity<String> getDraftById(
            @PathVariable String taskId,
            @RequestHeader(value = "Authorization", required = false) String authHeader);

    @GetMapping("/api/ai/drafts/learning-paths")
    ResponseEntity<String> getLearningPathDrafts(
            @RequestHeader(value = "Authorization", required = false) String authHeader);

    @PostMapping("/api/ai/drafts/{taskId}/approve-exercise")
    ResponseEntity<String> approveExerciseDraft(
            @PathVariable String taskId,
            @RequestHeader(value = "Authorization", required = false) String authHeader);

    @PostMapping("/api/ai/drafts/{taskId}/approve-learning-path")
    ResponseEntity<String> approveLearningPathDraft(
            @PathVariable String taskId,
            @RequestHeader(value = "Authorization", required = false) String authHeader);

    @PostMapping("/api/ai/drafts/{taskId}/reject")
    ResponseEntity<String> rejectDraft(
            @PathVariable String taskId,
            @RequestParam(required = false, defaultValue = "No reason provided") String reason,
            @RequestHeader(value = "Authorization", required = false) String authHeader);
}
