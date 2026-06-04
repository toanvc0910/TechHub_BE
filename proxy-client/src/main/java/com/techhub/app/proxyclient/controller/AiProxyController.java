package com.techhub.app.proxyclient.controller;

import com.techhub.app.proxyclient.client.AiServiceClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

@RestController
@RequestMapping("/api/proxy/ai")
@RequiredArgsConstructor
@Slf4j
public class AiProxyController {

    private final AiServiceClient aiServiceClient;
    private final RestTemplate restTemplate;

    @Value("${ai.service.direct-url:${AI_SERVICE_DIRECT_URL:}}")
    private String aiServiceDirectUrl;

    @PostMapping("/exercises/generate")
    public ResponseEntity<String> generateExercises(@RequestBody Object request,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        return aiServiceClient.generateExercises(request, authHeader);
    }

    @PostMapping("/learning-paths/generate")
    public ResponseEntity<String> generateLearningPaths(@RequestBody Object request,
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            HttpServletRequest httpRequest) {
        log.info("Proxy AI learning-path generate directUrlConfigured={} path={} userIdAttr={}",
                StringUtils.hasText(aiServiceDirectUrl),
                httpRequest.getRequestURI(),
                httpRequest.getAttribute("userId"));
        if (!StringUtils.hasText(aiServiceDirectUrl)) {
            log.info("Proxy AI learning-path generate using Feign/Eureka target=AI-SERVICE");
            return aiServiceClient.generateLearningPath(request, authHeader);
        }

        HttpHeaders headers = buildAiHeaders(authHeader, httpRequest);
        log.info("Proxy AI learning-path generate using direct URL target={}", aiServiceDirectUrl);
        return restTemplate.postForEntity(
                aiServiceDirectUrl + "/api/ai/learning-paths/generate",
                new HttpEntity<>(request, headers),
                String.class);
    }

    private HttpHeaders buildAiHeaders(String authHeader, HttpServletRequest request) {
        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.CONTENT_TYPE, "application/json");
        if (authHeader != null && !authHeader.isBlank()) {
            headers.add(HttpHeaders.AUTHORIZATION, authHeader);
        }

        Object userId = request.getAttribute("userId");
        if (userId != null) {
            headers.add("X-User-Id", userId.toString());
        }

        Object userEmail = request.getAttribute("userEmail");
        if (userEmail != null) {
            headers.add("X-User-Email", userEmail.toString());
        }

        Object userRoles = request.getAttribute("userRoles");
        if (userRoles instanceof List<?>) {
            headers.add("X-User-Roles", String.join(",", ((List<?>) userRoles).stream().map(String::valueOf).toList()));
        } else if (userRoles != null) {
            headers.add("X-User-Roles", userRoles.toString());
        }
        headers.add("X-Request-Source", "proxy-client");
        return headers;
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

    @GetMapping("/recommendations/history")
    public ResponseEntity<String> getRecommendationHistory(
            @RequestParam java.util.UUID userId,
            @RequestParam(required = false) String mode,
            @RequestParam(required = false, defaultValue = "20") Integer limit,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        return aiServiceClient.getRecommendationHistory(userId, mode, limit, authHeader);
    }

    @PostMapping("/chat/messages")
    public ResponseEntity<String> chat(@RequestBody Object request,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        return aiServiceClient.chat(request, authHeader);
    }

    @GetMapping("/admin/provider-config")
    public ResponseEntity<String> getProviderConfig(
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        return aiServiceClient.getProviderConfig(authHeader);
    }

    @PostMapping("/admin/provider-config")
    public ResponseEntity<String> updateProviderConfig(
            @RequestBody Object request,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        return aiServiceClient.updateProviderConfig(request, authHeader);
    }

    @GetMapping("/admin/provider-health")
    public ResponseEntity<String> getProviderHealth(
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        return aiServiceClient.getProviderHealth(authHeader);
    }

    @GetMapping("/admin/available-models")
    public ResponseEntity<String> getAvailableModels(
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        return aiServiceClient.getAvailableModels(authHeader);
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

    @GetMapping("/admin/runtime-stats")
    public ResponseEntity<String> getRuntimeStats(
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        return aiServiceClient.getRuntimeStats(authHeader);
    }

    @GetMapping("/admin/data-contract")
    public ResponseEntity<String> getDataContract(
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        return aiServiceClient.getDataContract(authHeader);
    }

    @GetMapping("/admin/data-contract/validate")
    public ResponseEntity<String> validateDataContract(
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        return aiServiceClient.validateDataContract(authHeader);
    }

    @PostMapping("/admin/ingest-file-uploaded")
    public ResponseEntity<String> ingestFileUploaded(
            @RequestBody Object request,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        return aiServiceClient.ingestFileUploaded(request, authHeader);
    }

    // ============================================
    // LANGFUSE ANALYTICS
    // ============================================

    @GetMapping("/admin/langfuse-traces")
    public ResponseEntity<String> getLangfuseTraces(
            @RequestParam(required = false, defaultValue = "1") Integer page,
            @RequestParam(required = false, defaultValue = "50") Integer limit,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        return aiServiceClient.getLangfuseTraces(page, limit, authHeader);
    }

    @GetMapping("/admin/langfuse-trace/{traceId}")
    public ResponseEntity<String> getLangfuseTraceDetail(
            @PathVariable String traceId,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        return aiServiceClient.getLangfuseTraceDetail(traceId, authHeader);
    }

    @GetMapping("/admin/langfuse-analytics")
    public ResponseEntity<String> getLangfuseAnalytics(
            @RequestParam(required = false, defaultValue = "7") Integer days,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        return aiServiceClient.getLangfuseAnalytics(days, authHeader);
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

    @PostMapping("/drafts/exercises/batch")
    public ResponseEntity<String> getExerciseDraftsBatch(
            @RequestBody Object request,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        return aiServiceClient.getExerciseDraftsBatch(request, authHeader);
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

    @PostMapping("/chat/sessions")
    public ResponseEntity<String> createSession(
            @RequestParam java.util.UUID userId,
            @RequestParam(required = false) String mode,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        return aiServiceClient.createSession(userId, mode, authHeader);
    }

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
