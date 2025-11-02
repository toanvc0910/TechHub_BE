package com.techhub.app.courseservice.dto;

import com.techhub.app.courseservice.enums.TestCaseVisibility;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ExerciseTestCaseDto {

    private UUID id;
    private Integer orderIndex;
    private TestCaseVisibility visibility;
    private String input;
    private String expectedOutput;
    private Float weight;
    private Integer timeoutSeconds;
    private Boolean sample;
    private String metadata;
}
