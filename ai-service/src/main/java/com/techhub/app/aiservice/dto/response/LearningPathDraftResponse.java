package com.techhub.app.aiservice.dto.response;

import com.techhub.app.aiservice.enums.AiTaskStatus;
import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.UUID;

@Data
@Builder
public class LearningPathDraftResponse {
    private UUID taskId;
    private AiTaskStatus status;
    private String title;
    private Object nodes;
    private Object edges;
    private List<UUID> courseIds;
}
