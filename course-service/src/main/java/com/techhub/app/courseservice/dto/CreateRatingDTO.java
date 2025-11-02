package com.techhub.app.courseservice.dto;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateRatingDTO {
    @NotNull(message = "User ID is required")
    private UUID userId;

    @NotNull(message = "Target ID is required")
    private UUID targetId;

    @NotBlank(message = "Target type is required")
    private String targetType;

    @NotNull(message = "Score is required")
    @Min(value = 1, message = "Score must be at least 1")
    @Max(value = 5, message = "Score must be at most 5")
    private Double score;

    private String comment;
}
