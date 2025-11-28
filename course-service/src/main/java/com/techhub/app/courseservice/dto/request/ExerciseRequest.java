package com.techhub.app.courseservice.dto.request;

import com.techhub.app.courseservice.dto.ExerciseTestCaseDto;
import com.techhub.app.courseservice.enums.ExerciseType;
import lombok.Getter;
import lombok.Setter;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.util.List;

@Getter
@Setter
public class ExerciseRequest {

    @NotNull
    private ExerciseType type;

    @NotBlank
    private String question;

    private Object options;

    private Integer orderIndex;

    private List<ExerciseTestCaseDto> testCases;
}
