package com.techhub.app.learningpathservice.mapper;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.techhub.app.learningpathservice.dto.request.CreateLearningPathRequest;
import com.techhub.app.learningpathservice.dto.request.UpdateLearningPathRequest;
import com.techhub.app.learningpathservice.dto.response.LearningPathResponse;
import com.techhub.app.learningpathservice.entity.LearningPath;

@Component
public class LearningPathMapper {

    public LearningPath toEntity(CreateLearningPathRequest request) {
        LearningPath entity = new LearningPath();
        entity.setTitle(request.getTitle());
        entity.setDescription(request.getDescription());
        entity.setSkills(request.getSkills());
        entity.setCreated(LocalDateTime.now());
        entity.setUpdated(LocalDateTime.now());
        entity.setIsActive("Y");
        return entity;
    }

    public LearningPathResponse toResponse(LearningPath entity) {
        return LearningPathResponse.builder()
                .id(entity.getId())
                .title(entity.getTitle())
                .description(entity.getDescription())
                .skills(entity.getSkills())
                .created(entity.getCreated())
                .updated(entity.getUpdated())
                .createdBy(entity.getCreatedBy())
                .build();
    }

    public List<LearningPathResponse> toResponseList(List<LearningPath> entities) {
        return entities.stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    public void updateEntity(UpdateLearningPathRequest request, LearningPath entity) {
        if (request.getTitle() != null) {
            entity.setTitle(request.getTitle());
        }
        if (request.getDescription() != null) {
            entity.setDescription(request.getDescription());
        }
        if (request.getSkills() != null) {
            entity.setSkills(request.getSkills());
        }
        entity.setUpdated(LocalDateTime.now());
    }
}
