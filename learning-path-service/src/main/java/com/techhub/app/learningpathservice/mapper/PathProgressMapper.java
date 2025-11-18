package com.techhub.app.learningpathservice.mapper;

import com.techhub.app.learningpathservice.dto.PathProgressResponseDTO;
import com.techhub.app.learningpathservice.dto.UpdateProgressRequestDTO;
import com.techhub.app.learningpathservice.entity.PathProgress;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class PathProgressMapper {

    public PathProgress toEntity(UpdateProgressRequestDTO dto) {
        PathProgress entity = new PathProgress();
        entity.setUserId(dto.getUserId());
        entity.setPathId(dto.getPathId());
        entity.setCompletion(dto.getCompletion());
        entity.setMilestones(dto.getMilestones());
        entity.setUpdatedBy(dto.getUpdatedBy());
        return entity;
    }

    public void updateEntity(PathProgress entity, UpdateProgressRequestDTO dto) {
        entity.setCompletion(dto.getCompletion());
        entity.setMilestones(dto.getMilestones());
        entity.setUpdatedBy(dto.getUpdatedBy());
    }

    public PathProgressResponseDTO toDTO(PathProgress entity) {
        return PathProgressResponseDTO.builder()
                .id(entity.getId())
                .userId(entity.getUserId())
                .pathId(entity.getPathId())
                .pathTitle(entity.getLearningPath() != null ? entity.getLearningPath().getTitle() : null)
                .completion(entity.getCompletion())
                .milestones(entity.getMilestones())
                .created(entity.getCreated())
                .updated(entity.getUpdated())
                .isActive(entity.getIsActive())
                .build();
    }

    public List<PathProgressResponseDTO> toDTOList(List<PathProgress> entities) {
        return entities.stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }
}
