package com.techhub.app.learningpathservice.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LearningPathStatsResponse {
    private UUID pathId;
    private Integer totalEnrolled;
    private Double averageCompletion;
}
