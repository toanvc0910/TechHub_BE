package com.techhub.app.aiservice.dto.request;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.util.List;
import java.util.UUID;

@Data
public class LearningPathGenerateRequest {

    @NotBlank
    private String goal;

    @NotBlank
    private String timeframe; // e.g. "3-6 months"

    @NotBlank
    private String language = "vi";

    @NotBlank
    private String currentLevel;

    @NotBlank
    private String targetLevel;

    @NotNull
    private UUID userId;

    private List<UUID> preferredCourseIds;
    private boolean includePositions = true;
    private boolean includeProjects = true;
}
