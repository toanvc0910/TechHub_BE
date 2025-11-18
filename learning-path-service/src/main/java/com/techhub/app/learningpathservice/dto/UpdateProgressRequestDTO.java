package com.techhub.app.learningpathservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import java.util.Map;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateProgressRequestDTO {

    @NotNull(message = "User ID is required")
    private UUID userId;

    @NotNull(message = "Path ID is required")
    private UUID pathId;

    @NotNull(message = "Completion is required")
    @Min(value = 0, message = "Completion must be at least 0")
    @Max(value = 100, message = "Completion must be at most 100")
    private Float completion;

    private Map<String, Object> milestones;

    private UUID updatedBy;
}
