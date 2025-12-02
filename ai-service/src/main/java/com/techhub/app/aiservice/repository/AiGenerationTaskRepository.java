package com.techhub.app.aiservice.repository;

import com.techhub.app.aiservice.entity.AiGenerationTask;
import com.techhub.app.aiservice.enums.AiTaskStatus;
import com.techhub.app.aiservice.enums.AiTaskType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AiGenerationTaskRepository extends JpaRepository<AiGenerationTask, UUID> {

    List<AiGenerationTask> findByTaskTypeOrderByCreatedDesc(AiTaskType taskType);

    /**
     * Lấy tất cả drafts theo lesson_id (cho exercise generation)
     * Sắp xếp mới nhất trước để FE lấy draft mới nhất
     */
    List<AiGenerationTask> findByTargetReferenceAndStatusAndTaskTypeOrderByCreatedDesc(
            String targetReference, AiTaskStatus status, AiTaskType taskType);

    /**
     * Lấy draft mới nhất cho 1 lesson
     */
    Optional<AiGenerationTask> findFirstByTargetReferenceAndStatusAndTaskTypeOrderByCreatedDesc(
            String targetReference, AiTaskStatus status, AiTaskType taskType);

    /**
     * Lấy tất cả drafts cho nhiều lessons cùng lúc
     * Dùng cho việc lấy tất cả exercise drafts của 1 course
     */
    List<AiGenerationTask> findByTargetReferenceInAndStatusAndTaskTypeOrderByCreatedDesc(
            Collection<String> targetReferences, AiTaskStatus status, AiTaskType taskType);
}
