package com.techhub.app.aiservice.dto.response;

import com.techhub.app.aiservice.enums.AiTaskStatus;
import lombok.Builder;
import lombok.Data;

import java.util.UUID;

@Data
@Builder
public class AiExerciseGenerationResponse {
    private UUID taskId;
    private AiTaskStatus status;
    private Object drafts;
    private String message;
}
