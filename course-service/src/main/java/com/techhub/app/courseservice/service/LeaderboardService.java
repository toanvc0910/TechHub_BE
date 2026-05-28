package com.techhub.app.courseservice.service;

import com.techhub.app.courseservice.client.UserServiceClient;
import com.techhub.app.courseservice.dto.response.LeaderboardEntryResponse;
import com.techhub.app.courseservice.repository.SubmissionRepository;
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
    private final UserServiceClient userServiceClient;

    public List<LeaderboardEntryResponse> getLessonLeaderboard(UUID lessonId, int limit) {
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
            Map<String, Object> resp = userServiceClient.getUsersBatch(ids);
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

    private OffsetDateTime toOffsetDateTime(Object value) {
        if (value == null) return null;
        if (value instanceof OffsetDateTime) return (OffsetDateTime) value;
        if (value instanceof Timestamp) return ((Timestamp) value).toInstant().atOffset(ZoneOffset.UTC);
        return null;
    }
}
