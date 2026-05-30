package com.techhub.app.courseservice.service;

import com.techhub.app.courseservice.client.UserServiceClient;
import com.techhub.app.courseservice.dto.response.LeaderboardEntryResponse;
import com.techhub.app.courseservice.entity.Lesson;
import com.techhub.app.courseservice.repository.LessonRepository;
import com.techhub.app.courseservice.repository.SubmissionRepository;
import com.techhub.app.commonservice.context.UserContext;
import com.techhub.app.commonservice.exception.ForbiddenException;
import com.techhub.app.commonservice.exception.NotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.sql.Timestamp;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class LeaderboardService {

    private final SubmissionRepository submissionRepository;
    private final LessonRepository lessonRepository;
    private final UserServiceClient userServiceClient;

    public List<LeaderboardEntryResponse> getLessonLeaderboard(UUID courseId, UUID lessonId, int limit) {
        validateLesson(courseId, lessonId);
        int safeLimit = Math.min(Math.max(limit, 1), 50);
        List<Object[]> rows = submissionRepository.findLessonLeaderboard(lessonId, safeLimit);
        if (rows.isEmpty()) return Collections.emptyList();

        List<UUID> userIds = new ArrayList<>();
        for (Object[] row : rows) {
            userIds.add((UUID) row[0]);
        }

        Map<UUID, Map<String, Object>> userInfo = fetchUserInfo(userIds);

        List<LeaderboardEntryResponse> result = new ArrayList<>(rows.size());
        int rank = 1;
        for (Object[] row : rows) {
            UUID userId = (UUID) row[0];
            double score = ((Number) row[1]).doubleValue();
            long attempts = ((Number) row[2]).longValue();
            OffsetDateTime firstAt = toOffsetDateTime(row[3]);

            Map<String, Object> u = userInfo.getOrDefault(userId, Collections.emptyMap());
            result.add(LeaderboardEntryResponse.builder()
                    .rank(rank++)
                    .userId(userId)
                    .username((String) u.get("username"))
                    .avatar((String) u.get("avatar"))
                    .score(score)
                    .attempts(attempts)
                    .firstAt(firstAt)
                    .build());
        }
        return result;
    }

    @SuppressWarnings("unchecked")
    private Map<UUID, Map<String, Object>> fetchUserInfo(List<UUID> ids) {
        try {
            Map<String, Object> resp = userServiceClient.getUsersBatch(
                    ids,
                    currentUserId(),
                    UserContext.getCurrentUserEmail(),
                    currentUserRoles(),
                    "course-service");
            Object data = resp == null ? null : resp.get("data");
            if (!(data instanceof List)) return Collections.emptyMap();
            Map<UUID, Map<String, Object>> map = new HashMap<>();
            for (Object item : (List<Object>) data) {
                if (!(item instanceof Map)) continue;
                Map<String, Object> m = (Map<String, Object>) item;
                Object idObj = m.get("id");
                if (idObj == null) continue;
                UUID id = idObj instanceof UUID ? (UUID) idObj : UUID.fromString(idObj.toString());
                map.put(id, m);
            }
            return map;
        } catch (Exception ex) {
            log.warn("[Leaderboard] Failed fetching user info batch: {}", ex.getMessage());
            return Collections.emptyMap();
        }
    }

    private void validateLesson(UUID courseId, UUID lessonId) {
        Lesson lesson = lessonRepository.findById(lessonId)
                .orElseThrow(() -> new NotFoundException("Lesson not found"));
        if (lesson.getChapter() == null || lesson.getChapter().getCourse() == null) {
            throw new NotFoundException("Lesson is not attached to a course");
        }
        if (!lesson.getChapter().getCourse().getId().equals(courseId)) {
            throw new ForbiddenException("Lesson does not belong to the specified course");
        }
        if (lesson.getIsActive() != null && !lesson.getIsActive()) {
            throw new NotFoundException("Lesson is not active");
        }
    }

    private String currentUserId() {
        UUID userId = UserContext.getCurrentUserId();
        return userId != null ? userId.toString() : null;
    }

    private String currentUserRoles() {
        List<String> roles = UserContext.getCurrentUserRoles();
        return roles == null ? null : String.join(",", roles);
    }

    private OffsetDateTime toOffsetDateTime(Object value) {
        if (value == null) return null;
        if (value instanceof OffsetDateTime) return (OffsetDateTime) value;
        if (value instanceof Timestamp) return ((Timestamp) value).toInstant().atOffset(ZoneOffset.UTC);
        return null;
    }
}
