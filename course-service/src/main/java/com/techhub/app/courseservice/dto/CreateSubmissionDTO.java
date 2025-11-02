package com.techhub.app.courseservice.dto;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateSubmissionDTO {
    @NotNull(message = "User ID is required")
    private UUID userId;

    @NotNull(message = "Exercise ID is required")
    private UUID exerciseId;

    @NotBlank(message = "Answer is required")
    private String answer;
}
