package com.techhub.app.learningpathservice.dto.request;

import java.util.UUID;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CourseOrderRequest {
    @NotNull(message = "Course ID is required")
    private UUID courseId;

    @NotNull(message = "Order is required")
    @Min(value = 1, message = "Order must be greater than 0")
    private Integer order;
}
