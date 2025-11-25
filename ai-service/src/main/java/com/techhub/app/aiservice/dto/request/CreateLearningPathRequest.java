package com.techhub.app.aiservice.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateLearningPathRequest {

    private String title;
    private String description;
    private List<String> skills;
    private List<LayoutEdge> layoutEdges;
    private UUID createdBy;
    private UUID updatedBy;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LayoutEdge {
        private String source;
        private String target;
    }
}
