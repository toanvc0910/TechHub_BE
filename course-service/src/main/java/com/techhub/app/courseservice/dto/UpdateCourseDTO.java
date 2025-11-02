package com.techhub.app.courseservice.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdateCourseDTO {
    private String title;
    private String description;
    private Double price;
    private String status;
    private List<String> categories;
    private List<String> tags;
}

