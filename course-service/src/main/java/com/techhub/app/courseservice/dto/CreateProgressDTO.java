package com.techhub.app.courseservice.dto;

import javax.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateProgressDTO {
    @NotNull(message = "User ID is required")
    private UUID userId;

    @NotNull(message = "Lesson ID is required")
    private UUID lessonId;

    @NotNull(message = "Completion is required")
    private Double completion;
}
