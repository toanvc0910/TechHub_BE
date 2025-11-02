package com.techhub.app.courseservice.dto.request;

import lombok.Getter;
import lombok.Setter;

import javax.validation.constraints.NotNull;

@Getter
@Setter
public class ExerciseSubmissionRequest {

    @NotNull
    private String answer;

    private Object submissionData;
}
