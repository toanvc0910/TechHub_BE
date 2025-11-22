package com.techhub.app.aiservice.service;

import com.techhub.app.aiservice.dto.response.ApproveExerciseDraftResponse;
import com.techhub.app.aiservice.dto.response.ApproveLearningPathDraftResponse;

import java.util.UUID;

/**
 * Service để approve AI drafts và lưu vào DB chính
 */
public interface AiDraftApprovalService {

    /**
     * Approve exercise draft và lưu vào Course Service
     * 
     * @param taskId UUID của draft trong ai_generation_tasks
     * @return Response với thông tin exercise đã lưu
     */
    ApproveExerciseDraftResponse approveExerciseDraft(UUID taskId);

    /**
     * Approve learning path draft và lưu vào Learning Path Service
     * 
     * @param taskId UUID của draft trong ai_generation_tasks
     * @return Response với thông tin learning path đã lưu
     */
    ApproveLearningPathDraftResponse approveLearningPathDraft(UUID taskId);

    /**
     * Reject draft (chỉ update status thành REJECTED)
     * 
     * @param taskId UUID của draft
     * @param reason Lý do reject
     */
    void rejectDraft(UUID taskId, String reason);
}
