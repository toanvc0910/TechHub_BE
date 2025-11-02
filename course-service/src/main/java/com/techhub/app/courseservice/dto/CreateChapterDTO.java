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
public class CreateChapterDTO {
    @NotBlank(message = "Title is required")
    private String title;

    @NotNull(message = "Order is required")
    private Integer order;

    @NotNull(message = "Course ID is required")
    private UUID courseId;
}
