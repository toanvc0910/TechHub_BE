package com.techhub.app.learningpathservice.dto.response;

import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LeaderboardEntry {

    private Integer rank;
    private UUID userId;
    private String username;
    private String avatarUrl;

    private Double score;
    private Integer completedCourses;
    private Float averageCompletion;
}