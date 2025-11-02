package com.techhub.app.courseservice.dto;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateExerciseDTO {
    @NotBlank(message = "Type is required")
    private String type;

    @NotBlank(message = "Question is required")
    private String question;

    private String testCases;

    @NotNull(message = "Lesson ID is required")
    private UUID lessonId;

    private List<String> options;
}
