package com.techhub.app.learningpathservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotEmpty;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AddCoursesToPathRequestDTO {

    @NotEmpty(message = "Courses list cannot be empty")
    private List<CourseInPathDTO> courses;
}
