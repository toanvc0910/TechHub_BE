package com.techhub.app.aiservice.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LearningPathNode {
    private String id;
    private String type; // e.g., "courseNode", "milestoneNode"
    private NodeData data;
    private Position position;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class NodeData {
        private String label;
        private UUID courseId;
        private String status; // LOCKED, UNLOCKED, COMPLETED
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Position {
        private int x;
        private int y;
    }
}
