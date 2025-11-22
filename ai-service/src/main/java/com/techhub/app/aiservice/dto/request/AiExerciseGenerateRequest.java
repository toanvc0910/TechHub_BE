package com.techhub.app.aiservice.dto.request;

import com.techhub.app.aiservice.enums.DifficultyLevel;
import com.techhub.app.aiservice.enums.ExerciseFormat;
import lombok.Data;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.util.Set;
import java.util.UUID;

@Data
public class AiExerciseGenerateRequest {

    @NotNull
    private UUID courseId;

    @NotNull
    private UUID lessonId;

    @NotBlank
    private String language = "vi";

    @NotEmpty
    private Set<DifficultyLevel> difficulties;

    @NotEmpty
    private Set<ExerciseFormat> formats;

    @Min(1)
    private int variants = 1;

    private boolean includeExplanations = true;
    private boolean includeTestCases = true;

    private String customInstruction;

    // Fields required by AiExerciseServiceImpl
    private int count = 5;
    private String type = "MCQ"; // Default to MCQ
    private String difficulty = "BEGINNER"; // Default to BEGINNER
}
