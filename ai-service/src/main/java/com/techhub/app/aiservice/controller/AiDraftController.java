package com.techhub.app.aiservice.controller;

import com.techhub.app.aiservice.dto.response.AiDraftListResponse;
import com.techhub.app.aiservice.dto.response.ApproveExerciseDraftResponse;
import com.techhub.app.aiservice.dto.response.ApproveLearningPathDraftResponse;
import com.techhub.app.aiservice.entity.AiGenerationTask;
import com.techhub.app.aiservice.enums.AiTaskStatus;
import com.techhub.app.aiservice.enums.AiTaskType;
import com.techhub.app.aiservice.repository.AiGenerationTaskRepository;
import com.techhub.app.aiservice.service.AiDraftApprovalService;
import com.techhub.app.commonservice.payload.GlobalResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Controller để quản lý AI drafts (exercises, learning paths chờ approve)
 */
@RestController
@RequestMapping("/api/ai/drafts")
@Validated
@RequiredArgsConstructor
@Slf4j
public class AiDraftController {

    private final AiGenerationTaskRepository aiGenerationTaskRepository;
    private final AiDraftApprovalService aiDraftApprovalService;

    /**
     * Get tất cả exercise drafts cho 1 lesson
     * Trường hợp: Admin click vào lesson → hiển thị tất cả drafts đã tạo
     * 
     * GET /api/ai/drafts/exercises?lessonId={lessonId}
     */
    @GetMapping("/exercises")
    public ResponseEntity<GlobalResponse<List<AiDraftListResponse>>> getExerciseDrafts(
            @RequestParam UUID lessonId,
            HttpServletRequest servletRequest) {

        log.info("📋 Getting exercise drafts for lesson: {}", lessonId);

        List<AiGenerationTask> drafts = aiGenerationTaskRepository
                .findByTargetReferenceAndStatusAndTaskTypeOrderByCreatedDesc(
                        lessonId.toString(),
                        AiTaskStatus.DRAFT,
                        AiTaskType.EXERCISE_GENERATION);

        List<AiDraftListResponse> response = drafts.stream()
                .map(task -> AiDraftListResponse.builder()
                        .taskId(task.getId())
                        .taskType(task.getTaskType())
                        .status(task.getStatus())
                        .targetReference(task.getTargetReference())
                        .resultPayload(task.getResultPayload())
                        .createdAt(task.getCreated())
                        .build())
                .collect(Collectors.toList());

        return ResponseEntity.ok(
                GlobalResponse.success("Found " + response.size() + " draft(s)", response)
                        .withPath(servletRequest.getRequestURI()));
    }

    /**
     * Get draft mới nhất cho 1 lesson (most common use case)
     * 
     * GET /api/ai/drafts/exercises/latest?lessonId={lessonId}
     */
    @GetMapping("/exercises/latest")
    public ResponseEntity<GlobalResponse<AiDraftListResponse>> getLatestExerciseDraft(
            @RequestParam UUID lessonId,
            HttpServletRequest servletRequest) {

        log.info("📋 Getting latest exercise draft for lesson: {}", lessonId);

        AiGenerationTask task = aiGenerationTaskRepository
                .findFirstByTargetReferenceAndStatusAndTaskTypeOrderByCreatedDesc(
                        lessonId.toString(),
                        AiTaskStatus.DRAFT,
                        AiTaskType.EXERCISE_GENERATION)
                .orElseThrow(() -> new RuntimeException("No draft found for lesson: " + lessonId));

        AiDraftListResponse response = AiDraftListResponse.builder()
                .taskId(task.getId())
                .taskType(task.getTaskType())
                .status(task.getStatus())
                .targetReference(task.getTargetReference())
                .resultPayload(task.getResultPayload())
                .createdAt(task.getCreated())
                .build();

        return ResponseEntity.ok(
                GlobalResponse.success("Latest draft retrieved", response)
                        .withPath(servletRequest.getRequestURI()));
    }

    /**
     * Get chi tiết 1 draft bất kỳ theo taskId
     * 
     * GET /api/ai/drafts/{taskId}
     */
    @GetMapping("/{taskId}")
    public ResponseEntity<GlobalResponse<AiDraftListResponse>> getDraftById(
            @PathVariable UUID taskId,
            HttpServletRequest servletRequest) {

        log.info("📋 Getting draft detail: {}", taskId);

        AiGenerationTask task = aiGenerationTaskRepository.findById(taskId)
                .orElseThrow(() -> new RuntimeException("Draft not found: " + taskId));

        AiDraftListResponse response = AiDraftListResponse.builder()
                .taskId(task.getId())
                .taskType(task.getTaskType())
                .status(task.getStatus())
                .targetReference(task.getTargetReference())
                .resultPayload(task.getResultPayload())
                .requestPayload(task.getRequestPayload())
                .prompt(task.getPrompt())
                .createdAt(task.getCreated())
                .build();

        return ResponseEntity.ok(
                GlobalResponse.success("Draft retrieved", response)
                        .withPath(servletRequest.getRequestURI()));
    }

    /**
     * Get tất cả learning path drafts
     * 
     * GET /api/ai/drafts/learning-paths
     */
    @GetMapping("/learning-paths")
    public ResponseEntity<GlobalResponse<List<AiDraftListResponse>>> getLearningPathDrafts(
            HttpServletRequest servletRequest) {

        log.info("📋 Getting all learning path drafts");

        List<AiGenerationTask> drafts = aiGenerationTaskRepository
                .findByTaskTypeOrderByCreatedDesc(AiTaskType.LEARNING_PATH_GENERATION)
                .stream()
                .filter(task -> task.getStatus() == AiTaskStatus.DRAFT)
                .collect(Collectors.toList());

        List<AiDraftListResponse> response = drafts.stream()
                .map(task -> AiDraftListResponse.builder()
                        .taskId(task.getId())
                        .taskType(task.getTaskType())
                        .status(task.getStatus())
                        .targetReference(task.getTargetReference())
                        .resultPayload(task.getResultPayload())
                        .createdAt(task.getCreated())
                        .build())
                .collect(Collectors.toList());

        return ResponseEntity.ok(
                GlobalResponse.success("Found " + response.size() + " draft(s)", response)
                        .withPath(servletRequest.getRequestURI()));
    }

    /**
     * Approve exercise draft
     * Update status thành APPROVED, admin sau đó dùng resultPayload để tạo exercise
     * 
     * POST /api/ai/drafts/{taskId}/approve-exercise
     */
    @PostMapping("/{taskId}/approve-exercise")
    public ResponseEntity<GlobalResponse<ApproveExerciseDraftResponse>> approveExerciseDraft(
            @PathVariable UUID taskId,
            HttpServletRequest servletRequest) {

        log.info("✅ Admin approving exercise draft: {}", taskId);

        ApproveExerciseDraftResponse response = aiDraftApprovalService.approveExerciseDraft(taskId);

        return ResponseEntity.ok(
                GlobalResponse.success("Exercise draft approved successfully", response)
                        .withStatus("DRAFT_APPROVED")
                        .withPath(servletRequest.getRequestURI()));
    }

    /**
     * Approve learning path draft
     * 
     * POST /api/ai/drafts/{taskId}/approve-learning-path
     */
    @PostMapping("/{taskId}/approve-learning-path")
    public ResponseEntity<GlobalResponse<ApproveLearningPathDraftResponse>> approveLearningPathDraft(
            @PathVariable UUID taskId,
            HttpServletRequest servletRequest) {

        log.info("✅ Admin approving learning path draft: {}", taskId);

        ApproveLearningPathDraftResponse response = aiDraftApprovalService.approveLearningPathDraft(taskId);

        return ResponseEntity.ok(
                GlobalResponse.success("Learning path draft approved successfully", response)
                        .withStatus("DRAFT_APPROVED")
                        .withPath(servletRequest.getRequestURI()));
    }

    /**
     * Reject draft với lý do
     * 
     * POST /api/ai/drafts/{taskId}/reject
     */
    @PostMapping("/{taskId}/reject")
    public ResponseEntity<GlobalResponse<Void>> rejectDraft(
            @PathVariable UUID taskId,
            @RequestParam(required = false, defaultValue = "No reason provided") String reason,
            HttpServletRequest servletRequest) {

        log.info("❌ Admin rejecting draft: {}", taskId);

        aiDraftApprovalService.rejectDraft(taskId, reason);

        return ResponseEntity.ok(
                GlobalResponse.<Void>success("Draft rejected successfully", null)
                        .withStatus("DRAFT_REJECTED")
                        .withPath(servletRequest.getRequestURI()));
    }
}
