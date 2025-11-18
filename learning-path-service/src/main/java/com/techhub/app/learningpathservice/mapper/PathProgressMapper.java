package com.techhub.app.learningpathservice.mapper;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.techhub.app.learningpathservice.dto.response.PathProgressResponse;
import com.techhub.app.learningpathservice.entity.PathProgress;

@Component
public class PathProgressMapper {

    public PathProgressResponse toResponse(PathProgress entity) {
        return PathProgressResponse.builder()
                .id(entity.getId())
                .userId(entity.getUserId())
                .pathId(entity.getPathId())
                .completion(entity.getCompletion())
                .created(entity.getCreated())
                .updated(entity.getUpdated())
                .build();
    }

    public List<PathProgressResponse> toResponseList(List<PathProgress> entities) {
        return entities.stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }
}
