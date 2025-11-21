package com.techhub.app.aiservice.dto.response;

import com.techhub.app.aiservice.enums.AiTaskStatus;
import com.techhub.app.aiservice.enums.RecommendationMode;
import lombok.Builder;
import lombok.Data;

import java.util.UUID;

@Data
@Builder
public class RecommendationResponse {
    private UUID recommendationId;
    private RecommendationMode mode;
    private AiTaskStatus status;
    private Object courses;
    private Object paths;
}
