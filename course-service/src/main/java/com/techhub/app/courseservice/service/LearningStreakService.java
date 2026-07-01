package com.techhub.app.courseservice.service;

import com.techhub.app.courseservice.dto.response.LearningStreakResponse;

import java.time.OffsetDateTime;
import java.util.UUID;

public interface LearningStreakService {

    LearningStreakResponse getCurrentUserStreak();

    LearningStreakResponse getStreakForUser(UUID userId);

    LearningStreakResponse recordActivity(UUID userId, OffsetDateTime activityAt);
}
