package com.techhub.app.aiservice.service.impl;

import com.techhub.app.aiservice.dto.response.ApproveExerciseDraftResponse;
import com.techhub.app.aiservice.dto.response.ApproveLearningPathDraftResponse;
import com.techhub.app.aiservice.entity.AiGenerationTask;
import com.techhub.app.aiservice.enums.AiTaskStatus;
import com.techhub.app.aiservice.enums.AiTaskType;
import com.techhub.app.aiservice.repository.AiGenerationTaskRepository;
import com.techhub.app.aiservice.service.AiDraftApprovalService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class AiDraftApprovalServiceImpl implements AiDraftApprovalService {

    private final AiGenerationTaskRepository aiGenerationTaskRepository;

    @Override
    @Transactional
    public ApproveExerciseDraftResponse approveExerciseDraft(UUID taskId) {
        log.info("✅ Approving exercise draft: {}", taskId);

        // 1. Lấy draft task
        AiGenerationTask task = aiGenerationTaskRepository.findById(taskId)
                .orElseThrow(() -> new RuntimeException("Draft not found: " + taskId));

        // 2. Validate
        if (task.getTaskType() != AiTaskType.EXERCISE_GENERATION) {
            throw new RuntimeException("Task is not an exercise generation task");
        }

        if (task.getStatus() != AiTaskStatus.DRAFT) {
            throw new RuntimeException("Task is not in DRAFT status, current: " + task.getStatus());
        }

        // 3. Update status thành APPROVED
        task.setStatus(AiTaskStatus.APPROVED);
        aiGenerationTaskRepository.save(task);

        // 4. Parse lesson_id từ targetReference
        UUID lessonId = UUID.fromString(task.getTargetReference());

        log.info("✅ Exercise draft approved. LessonId: {}, TaskId: {}", lessonId, taskId);
        log.info("📝 Admin can now use resultPayload to create exercises in Course Service");
        log.info("📝 API: PUT /api/courses/{{courseId}}/lessons/{}/exercise", lessonId);

        return ApproveExerciseDraftResponse.builder()
                .taskId(taskId)
                .lessonId(lessonId)
                .success(true)
                .message("Draft approved. Result payload is ready for Course Service API.")
                .build();
    }

    @Override
    @Transactional
    public ApproveLearningPathDraftResponse approveLearningPathDraft(UUID taskId) {
        log.info("✅ Approving learning path draft: {}", taskId);

        // 1. Lấy draft task
        AiGenerationTask task = aiGenerationTaskRepository.findById(taskId)
                .orElseThrow(() -> new RuntimeException("Draft not found: " + taskId));

        // 2. Validate
        if (task.getTaskType() != AiTaskType.LEARNING_PATH_GENERATION) {
            throw new RuntimeException("Task is not a learning path generation task");
        }

        if (task.getStatus() != AiTaskStatus.DRAFT) {
            throw new RuntimeException("Task is not in DRAFT status, current: " + task.getStatus());
        }

        // 3. Update status thành APPROVED
        task.setStatus(AiTaskStatus.APPROVED);
        aiGenerationTaskRepository.save(task);

        log.info("✅ Learning path draft approved. TaskId: {}", taskId);
        log.info("📝 Admin can now use resultPayload to create learning path");
        log.info("📝 API: POST /api/v1/learning-paths");

        return ApproveLearningPathDraftResponse.builder()
                .taskId(taskId)
                .success(true)
                .message("Draft approved. Result payload is ready for Learning Path Service API.")
                .build();
    }

    @Override
    @Transactional
    public void rejectDraft(UUID taskId, String reason) {
        log.info("❌ Rejecting draft: {}", taskId);

        AiGenerationTask task = aiGenerationTaskRepository.findById(taskId)
                .orElseThrow(() -> new RuntimeException("Draft not found: " + taskId));

        if (task.getStatus() != AiTaskStatus.DRAFT) {
            throw new RuntimeException("Task is not in DRAFT status, current: " + task.getStatus());
        }

        task.setStatus(AiTaskStatus.REJECTED);
        task.setErrorMessage("Rejected by admin: " + reason);
        aiGenerationTaskRepository.save(task);

        log.info("❌ Draft rejected: {}", taskId);
    }
}
