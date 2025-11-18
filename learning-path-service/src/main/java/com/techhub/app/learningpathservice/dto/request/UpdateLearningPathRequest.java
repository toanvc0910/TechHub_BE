package com.techhub.app.learningpathservice.dto.request;

import java.util.List;

import javax.validation.constraints.Size;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateLearningPathRequest {
    @Size(max = 255, message = "Title must not exceed 255 characters")
    private String title;
    @Size(max = 5000, message = "Description is too long")
    private String description;

    private List<String> skills;

}
