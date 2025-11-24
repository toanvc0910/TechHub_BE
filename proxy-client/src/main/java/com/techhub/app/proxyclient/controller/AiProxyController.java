package com.techhub.app.proxyclient.controller;

import com.techhub.app.proxyclient.client.AiServiceClient;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
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

    @PostMapping("/admin/reindex-lessons")
    public ResponseEntity<String> reindexLessons(
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        return aiServiceClient.reindexLessons(authHeader);
    }

    @PostMapping("/admin/reindex-all")
    public ResponseEntity<String> reindexAll(
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        return aiServiceClient.reindexAll(authHeader);
    }

    @GetMapping("/admin/qdrant-stats")
    public ResponseEntity<String> getQdrantStats(
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        return aiServiceClient.getQdrantStats(authHeader);
    }

    // ============================================
    // DRAFT MANAGEMENT
    // ============================================

    @GetMapping("/drafts/exercises")
    public ResponseEntity<String> getExerciseDrafts(
            @RequestParam java.util.UUID lessonId,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        return aiServiceClient.getExerciseDrafts(lessonId, authHeader);
    }

    @GetMapping("/drafts/exercises/latest")
    public ResponseEntity<String> getLatestExerciseDraft(
            @RequestParam java.util.UUID lessonId,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        return aiServiceClient.getLatestExerciseDraft(lessonId, authHeader);
    }

    @GetMapping("/drafts/{taskId}")
    public ResponseEntity<String> getDraftById(
            @PathVariable String taskId,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        return aiServiceClient.getDraftById(taskId, authHeader);
    }

    @GetMapping("/drafts/learning-paths")
    public ResponseEntity<String> getLearningPathDrafts(
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        return aiServiceClient.getLearningPathDrafts(authHeader);
    }

    @PostMapping("/drafts/{taskId}/approve-exercise")
    public ResponseEntity<String> approveExerciseDraft(
            @PathVariable String taskId,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        return aiServiceClient.approveExerciseDraft(taskId, authHeader);
    }

    @PostMapping("/drafts/{taskId}/approve-learning-path")
    public ResponseEntity<String> approveLearningPathDraft(
            @PathVariable String taskId,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        return aiServiceClient.approveLearningPathDraft(taskId, authHeader);
    }

    @PostMapping("/drafts/{taskId}/reject")
    public ResponseEntity<String> rejectDraft(
            @PathVariable String taskId,
            @RequestParam(required = false, defaultValue = "No reason provided") String reason,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        return aiServiceClient.rejectDraft(taskId, reason, authHeader);
    }

    // ============================================
    // CHAT SESSION HISTORY
    // ============================================

    @GetMapping("/chat/sessions")
    public ResponseEntity<String> getUserSessions(
            @RequestParam java.util.UUID userId,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        return aiServiceClient.getUserSessions(userId, authHeader);
    }

    @GetMapping("/chat/sessions/{sessionId}/messages")
    public ResponseEntity<String> getSessionMessages(
            @PathVariable java.util.UUID sessionId,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        return aiServiceClient.getSessionMessages(sessionId, authHeader);
    }

    @DeleteMapping("/chat/sessions/{sessionId}")
    public ResponseEntity<String> deleteSession(
            @PathVariable java.util.UUID sessionId,
            @RequestParam java.util.UUID userId,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        return aiServiceClient.deleteSession(sessionId, userId, authHeader);
    }
}
