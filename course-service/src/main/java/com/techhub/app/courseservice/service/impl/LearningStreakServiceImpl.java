package com.techhub.app.courseservice.service.impl;

import com.techhub.app.commonservice.context.UserContext;
import com.techhub.app.commonservice.exception.UnauthorizedException;
import com.techhub.app.courseservice.dto.response.LearningStreakResponse;
import com.techhub.app.courseservice.entity.LearningStreak;
import com.techhub.app.courseservice.repository.LearningStreakRepository;
import com.techhub.app.courseservice.service.LearningStreakService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class LearningStreakServiceImpl implements LearningStreakService {

    private static final ZoneId STREAK_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");

    private final LearningStreakRepository learningStreakRepository;

    @Override
    @Transactional(readOnly = true)
    public LearningStreakResponse getCurrentUserStreak() {
        UUID userId = UserContext.getCurrentUserId();
        if (userId == null) {
            throw new UnauthorizedException("Authentication required");
        }
        return getStreakForUser(userId);
    }

    @Override
    @Transactional(readOnly = true)
    public LearningStreakResponse getStreakForUser(UUID userId) {
        return learningStreakRepository.findByUserIdAndIsActiveTrue(userId)
                .map(this::mapToResponse)
                .orElseGet(() -> emptyResponse(userId));
    }

    @Override
    public LearningStreakResponse recordActivity(UUID userId, OffsetDateTime activityAt) {
        OffsetDateTime effectiveActivityAt = activityAt != null ? activityAt : OffsetDateTime.now();
        LocalDate activityDate = effectiveActivityAt.atZoneSameInstant(STREAK_ZONE).toLocalDate();

        LearningStreak streak = learningStreakRepository.findByUserIdAndIsActiveTrue(userId)
                .orElseGet(() -> {
                    LearningStreak entity = new LearningStreak();
                    entity.setUserId(userId);
                    entity.setCreatedBy(userId);
                    entity.setIsActive(true);
                    return entity;
                });

        LocalDate lastDate = streak.getLastActivityDate();
        if (lastDate == null) {
            streak.setCurrentStreak(1);
        } else if (activityDate.equals(lastDate)) {
            streak.setCurrentStreak(Math.max(1, safeInt(streak.getCurrentStreak())));
        } else if (activityDate.equals(lastDate.plusDays(1))) {
            streak.setCurrentStreak(safeInt(streak.getCurrentStreak()) + 1);
        } else if (activityDate.isAfter(lastDate.plusDays(1))) {
            streak.setCurrentStreak(1);
        }

        streak.setLongestStreak(Math.max(safeInt(streak.getLongestStreak()), safeInt(streak.getCurrentStreak())));
        if (lastDate == null || !activityDate.isBefore(lastDate)) {
            streak.setLastActivityDate(activityDate);
        }
        if (streak.getLastActivityAt() == null || effectiveActivityAt.isAfter(streak.getLastActivityAt())) {
            streak.setLastActivityAt(effectiveActivityAt);
        }
        streak.setUpdatedBy(userId);

        return mapToResponse(learningStreakRepository.save(streak));
    }

    private LearningStreakResponse mapToResponse(LearningStreak streak) {
        LocalDate today = LocalDate.now(STREAK_ZONE);
        return LearningStreakResponse.builder()
                .userId(streak.getUserId())
                .currentStreak(safeInt(streak.getCurrentStreak()))
                .longestStreak(safeInt(streak.getLongestStreak()))
                .lastActivityDate(streak.getLastActivityDate())
                .lastActivityAt(streak.getLastActivityAt())
                .completedToday(today.equals(streak.getLastActivityDate()))
                .build();
    }

    private LearningStreakResponse emptyResponse(UUID userId) {
        return LearningStreakResponse.builder()
                .userId(userId)
                .currentStreak(0)
                .longestStreak(0)
                .completedToday(false)
                .build();
    }

    private int safeInt(Integer value) {
        return value != null ? value : 0;
    }
}
