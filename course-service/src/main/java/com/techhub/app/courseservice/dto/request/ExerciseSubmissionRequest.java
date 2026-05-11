package com.techhub.app.courseservice.dto.request;

import lombok.Getter;
import lombok.Setter;

import javax.validation.constraints.NotNull;
import java.util.UUID;

@Getter
@Setter
public class ExerciseSubmissionRequest {

    private UUID exerciseId;

    @NotNull
    private String answer;

    private Object submissionData;
}
