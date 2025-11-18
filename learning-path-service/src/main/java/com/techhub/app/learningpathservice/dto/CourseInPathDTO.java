package com.techhub.app.learningpathservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotNull;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CourseInPathDTO {

    @NotNull(message = "Course ID is required")
    private UUID courseId;

    @NotNull(message = "Order is required")
    private Integer order;

    // Additional course details (fetched from course service)
    private String title;
    private String description;
    private String thumbnail;
    private String level;
    private String status;
}
