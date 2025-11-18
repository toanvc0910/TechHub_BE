package com.techhub.app.learningpathservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LearningPathResponseDTO {

    private UUID id;

    private String title;

    private String description;

    private List<String> skills;

    private List<CourseInPathDTO> courses;

    private OffsetDateTime created;

    private OffsetDateTime updated;

    private UUID createdBy;

    private UUID updatedBy;

    private Boolean isActive;
}
