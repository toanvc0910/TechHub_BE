package com.techhub.app.aiservice.repository;

import com.techhub.app.aiservice.entity.AiGenerationTask;
import com.techhub.app.aiservice.enums.AiTaskType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface AiGenerationTaskRepository extends JpaRepository<AiGenerationTask, UUID> {

    List<AiGenerationTask> findByTaskTypeOrderByCreatedDesc(AiTaskType taskType);
}
