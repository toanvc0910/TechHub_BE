package com.techhub.app.courseservice.dto.request;

import lombok.Getter;
import lombok.Setter;

import javax.validation.constraints.DecimalMax;
import javax.validation.constraints.DecimalMin;

@Getter
@Setter
public class LessonProgressRequest {

    @DecimalMin(value = "0.0")
    @DecimalMax(value = "1.0")
    private Float completion;

    private Boolean markComplete;
}
