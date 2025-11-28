package com.techhub.app.aiservice.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LearningPathEdge {
    private String id;
    private String source;
    private String target;
    private boolean animated;
    private String type; // e.g., "smoothstep"
    private String label;
}
