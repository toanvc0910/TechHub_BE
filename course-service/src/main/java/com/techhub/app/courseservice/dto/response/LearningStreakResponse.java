package com.techhub.app.courseservice.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LearningStreakResponse {

    private UUID userId;
    private Integer currentStreak;
    private Integer longestStreak;
    private LocalDate lastActivityDate;
    private OffsetDateTime lastActivityAt;
    private Boolean completedToday;
}
