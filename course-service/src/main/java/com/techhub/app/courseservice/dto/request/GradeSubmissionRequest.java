package com.techhub.app.courseservice.dto.request;

import com.techhub.app.courseservice.enums.SubmissionStatus;
import lombok.Getter;
import lombok.Setter;

import javax.validation.constraints.DecimalMax;
import javax.validation.constraints.DecimalMin;

/**
 * Instructor grading payload for a single submission. All fields are optional so
 * an instructor may post only a score, only written feedback, or both. When
 * {@code status} is omitted it is derived from the grade (>= 50 -> PASSED).
 */
@Getter
@Setter
public class GradeSubmissionRequest {

    @DecimalMin(value = "0", message = "Grade must be >= 0")
    @DecimalMax(value = "100", message = "Grade must be <= 100")
    private Float grade;

    private String feedback;

    private SubmissionStatus status;
}
