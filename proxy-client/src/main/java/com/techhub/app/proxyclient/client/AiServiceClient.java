package com.techhub.app.proxyclient.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
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

        @GetMapping("/api/ai/recommendations/history")
        ResponseEntity<String> getRecommendationHistory(
                        @RequestParam UUID userId,
                        @RequestParam(required = false) String mode,
                        @RequestParam(required = false, defaultValue = "20") Integer limit,
                        @RequestHeader(value = "Authorization", required = false) String authHeader);

        @PostMapping("/api/ai/chat/messages")
        ResponseEntity<String> chat(@RequestBody Object request,
                        @RequestHeader(value = "Authorization", required = false) String authHeader);

        @PostMapping("/api/ai/admin/reindex-courses")
        ResponseEntity<String> reindexCourses(
                        @RequestHeader(value = "Authorization", required = false) String authHeader);

        @PostMapping("/api/ai/admin/reindex-lessons")
        ResponseEntity<String> reindexLessons(
                        @RequestHeader(value = "Authorization", required = false) String authHeader);

        @PostMapping("/api/ai/admin/reindex-blogs")
        ResponseEntity<String> reindexBlogs(
                        @RequestHeader(value = "Authorization", required = false) String authHeader);

        @PostMapping("/api/ai/admin/reindex-data-contract")
        ResponseEntity<String> reindexDataContract(
                        @RequestHeader(value = "Authorization", required = false) String authHeader);

        @PostMapping("/api/ai/admin/reindex-all")
        ResponseEntity<String> reindexAll(@RequestHeader(value = "Authorization", required = false) String authHeader);

        @GetMapping("/api/ai/admin/qdrant-stats")
        ResponseEntity<String> getQdrantStats(
                        @RequestHeader(value = "Authorization", required = false) String authHeader);

        @GetMapping("/api/ai/admin/runtime-stats")
        ResponseEntity<String> getRuntimeStats(
                        @RequestHeader(value = "Authorization", required = false) String authHeader);

        @GetMapping("/api/ai/admin/data-contract")
        ResponseEntity<String> getDataContract(
                        @RequestHeader(value = "Authorization", required = false) String authHeader);

        @GetMapping("/api/ai/admin/data-contract/validate")
        ResponseEntity<String> validateDataContract(
                        @RequestHeader(value = "Authorization", required = false) String authHeader);

        @PostMapping("/api/ai/admin/data-contract/sync")
        ResponseEntity<String> syncDataContract(
                        @RequestHeader(value = "Authorization", required = false) String authHeader);

        @GetMapping("/api/ai/admin/langfuse-traces")
        ResponseEntity<String> getLangfuseTraces(
                        @RequestParam(required = false, defaultValue = "1") Integer page,
                        @RequestParam(required = false, defaultValue = "50") Integer limit,
                        @RequestHeader(value = "Authorization", required = false) String authHeader);

        @GetMapping("/api/ai/admin/langfuse-trace/{traceId}")
        ResponseEntity<String> getLangfuseTraceDetail(
                        @PathVariable String traceId,
                        @RequestHeader(value = "Authorization", required = false) String authHeader);

        @GetMapping("/api/ai/admin/langfuse-analytics")
        ResponseEntity<String> getLangfuseAnalytics(
                        @RequestParam(required = false, defaultValue = "7") Integer days,
                        @RequestHeader(value = "Authorization", required = false) String authHeader);

        @GetMapping("/api/ai/admin/provider-config")
        ResponseEntity<String> getProviderConfig(
                        @RequestHeader(value = "Authorization", required = false) String authHeader);

        @PostMapping("/api/ai/admin/provider-config")
        ResponseEntity<String> updateProviderConfig(
                        @RequestBody Object request,
                        @RequestHeader(value = "Authorization", required = false) String authHeader);

        @GetMapping("/api/ai/admin/provider-health")
        ResponseEntity<String> getProviderHealth(
                        @RequestHeader(value = "Authorization", required = false) String authHeader);

        @GetMapping("/api/ai/admin/available-models")
        ResponseEntity<String> getAvailableModels(
                        @RequestHeader(value = "Authorization", required = false) String authHeader);

        @PostMapping("/api/ai/admin/ingest-file-uploaded")
        ResponseEntity<String> ingestFileUploaded(
                        @RequestBody Object request,
                        @RequestHeader(value = "Authorization", required = false) String authHeader);

        // ============================================
        // DRAFT MANAGEMENT ENDPOINTS
        // ============================================

        @GetMapping("/api/ai/drafts/exercises")
        ResponseEntity<String> getExerciseDrafts(
                        @RequestParam UUID lessonId,
                        @RequestHeader(value = "Authorization", required = false) String authHeader);

        @PostMapping("/api/ai/drafts/exercises/batch")
        ResponseEntity<String> getExerciseDraftsBatch(
                        @RequestBody Object request,
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

        // ============================================
        // CHAT SESSION HISTORY ENDPOINTS
        // ============================================

        @PostMapping("/api/ai/chat/sessions")
        ResponseEntity<String> createSession(
                        @RequestParam UUID userId,
                        @RequestParam(required = false) String mode,
                        @RequestHeader(value = "Authorization", required = false) String authHeader);

        @GetMapping("/api/ai/chat/sessions")
        ResponseEntity<String> getUserSessions(
                        @RequestParam UUID userId,
                        @RequestHeader(value = "Authorization", required = false) String authHeader);

        @GetMapping("/api/ai/chat/sessions/{sessionId}/messages")
        ResponseEntity<String> getSessionMessages(
                        @PathVariable UUID sessionId,
                        @RequestHeader(value = "Authorization", required = false) String authHeader);

        @DeleteMapping("/api/ai/chat/sessions/{sessionId}")
        ResponseEntity<String> deleteSession(
                        @PathVariable UUID sessionId,
                        @RequestParam UUID userId,
                        @RequestHeader(value = "Authorization", required = false) String authHeader);
}
