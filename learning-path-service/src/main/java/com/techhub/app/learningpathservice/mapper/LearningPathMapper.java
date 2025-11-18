package com.techhub.app.learningpathservice.mapper;

import com.techhub.app.learningpathservice.dto.CourseInPathDTO;
import com.techhub.app.learningpathservice.dto.LearningPathRequestDTO;
import com.techhub.app.learningpathservice.dto.LearningPathResponseDTO;
import com.techhub.app.learningpathservice.entity.LearningPath;
import com.techhub.app.learningpathservice.entity.LearningPathCourse;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class LearningPathMapper {

    public LearningPath toEntity(LearningPathRequestDTO dto) {
        LearningPath entity = new LearningPath();
        entity.setTitle(dto.getTitle());
        entity.setDescription(dto.getDescription());
        entity.setSkills(dto.getSkills() != null ? dto.getSkills() : new ArrayList<>());
        entity.setCreatedBy(dto.getCreatedBy());
        entity.setUpdatedBy(dto.getUpdatedBy());
        return entity;
    }

    public void updateEntity(LearningPath entity, LearningPathRequestDTO dto) {
        entity.setTitle(dto.getTitle());
        entity.setDescription(dto.getDescription());
        entity.setSkills(dto.getSkills() != null ? dto.getSkills() : new ArrayList<>());
        entity.setUpdatedBy(dto.getUpdatedBy());
    }

    public LearningPathResponseDTO toDTO(LearningPath entity) {
        return LearningPathResponseDTO.builder()
                .id(entity.getId())
                .title(entity.getTitle())
                .description(entity.getDescription())
                .skills(entity.getSkills())
                .courses(mapCourses(entity.getCourses()))
                .created(entity.getCreated())
                .updated(entity.getUpdated())
                .createdBy(entity.getCreatedBy())
                .updatedBy(entity.getUpdatedBy())
                .isActive(entity.getIsActive())
                .build();
    }

    private List<CourseInPathDTO> mapCourses(List<LearningPathCourse> courses) {
        if (courses == null) {
            return new ArrayList<>();
        }
        return courses.stream()
                .map(lpc -> CourseInPathDTO.builder()
                        .courseId(lpc.getCourseId())
                        .order(lpc.getOrder())
                        .build())
                .collect(Collectors.toList());
    }

    public List<LearningPathResponseDTO> toDTOList(List<LearningPath> entities) {
        return entities.stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }
}
