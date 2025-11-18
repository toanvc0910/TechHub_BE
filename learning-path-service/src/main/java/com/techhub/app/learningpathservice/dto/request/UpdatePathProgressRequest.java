package com.techhub.app.learningpathservice.dto.request;

import java.util.Map;
import java.util.UUID;

import javax.validation.constraints.DecimalMax;
import javax.validation.constraints.DecimalMin;
import javax.validation.constraints.NotNull;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdatePathProgressRequest {
    @NotNull(message = "User Id is required")
    private UUID userId;

    @NotNull(message = "Path ID is required")
    private UUID pathId;

    @DecimalMin(value = "0.0", message = "Completion must be between 0 and 1")
    @DecimalMax(value = "1.0", message = "Completion must be between 0 and 1")
    private Float completion;

    private Map<String, Object> milestones;
}
