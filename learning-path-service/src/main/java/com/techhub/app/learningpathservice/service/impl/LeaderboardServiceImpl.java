package com.techhub.app.learningpathservice.service.impl;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.techhub.app.learningpathservice.dto.response.LeaderboardEntry;
import com.techhub.app.learningpathservice.dto.response.LeaderboardResponse;
import com.techhub.app.learningpathservice.entity.LearningPath;
import com.techhub.app.learningpathservice.entity.PathProgress;
import com.techhub.app.learningpathservice.enums.LeaderboardType;
import com.techhub.app.learningpathservice.exception.ResourceNotFoundException;
import com.techhub.app.learningpathservice.repository.LearningPathRepository;
import com.techhub.app.learningpathservice.repository.PathProgressRepository;
import com.techhub.app.learningpathservice.service.LeaderboardService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class LeaderboardServiceImpl implements LeaderboardService {

    private final PathProgressRepository progressRepository;
    private final LearningPathRepository learningPathRepository;

    @Override
    @Transactional(readOnly = true)
    public LeaderboardResponse getLeaderboard(LeaderboardType type, UUID referenceId, Integer limit) {
        log.debug("Getting leaderboard: type={}, referenceId={}, limit={}", type, referenceId, limit);

        switch (type) {
            case GLOBAL:
                return getGlobalLeaderboard(limit);
            case PATH:
                return getPathLeaderboard(referenceId, limit);
            case COURSE:
                throw new UnsupportedOperationException("Course leaderboard not supported in this service");
            default:
                throw new IllegalArgumentException("Unknown leaderboard type: " + type);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public LeaderboardResponse getGlobalLeaderboard(Integer limit) {
        log.debug("Getting global leaderboard with limit: {}", limit);

        // Get all progress records
        List<PathProgress> allProgress = progressRepository.findByIsActive("Y");

        // Group by user and calculate scores
        List<LeaderboardEntry> entries = allProgress.stream()
                .collect(Collectors.groupingBy(PathProgress::getUserId))
                .entrySet().stream()
                .map(entry -> {
                    UUID userId = entry.getKey();
                    List<PathProgress> userProgress = entry.getValue();

                    // Calculate metrics
                    double avgCompletion = userProgress.stream()
                            .mapToDouble(PathProgress::getCompletion)
                            .average()
                            .orElse(0.0);

                    int completedPaths = (int) userProgress.stream()
                            .filter(p -> p.getCompletion() >= 1.0f)
                            .count();

                    double score = (avgCompletion * 100) + (completedPaths * 10);

                    return LeaderboardEntry.builder()
                            .userId(userId)
                            .score(score)
                            .completedCourses(completedPaths)
                            .averageCompletion((float) avgCompletion)
                            .build();
                })
                .sorted(Comparator.comparing(LeaderboardEntry::getScore).reversed())
                .limit(limit != null ? limit : 100)
                .collect(Collectors.toList());

        // Set ranks
        for (int i = 0; i < entries.size(); i++) {
            entries.get(i).setRank(i + 1);
        }

        return LeaderboardResponse.builder()
                .type(LeaderboardType.GLOBAL)
                .entries(entries)
                .lastUpdated(LocalDateTime.now())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public LeaderboardResponse getPathLeaderboard(UUID pathId, Integer limit) {
        log.debug("Getting leaderboard for path: {}", pathId);

        // Verify path exists
        LearningPath path = learningPathRepository.findById(pathId)
                .orElseThrow(() -> new ResourceNotFoundException("Learning path not found with ID: " + pathId));

        // Get progress for this path
        List<PathProgress> pathProgress = progressRepository.findByPathIdAndIsActive(pathId, "Y");

        // Create leaderboard entries
        List<LeaderboardEntry> entries = pathProgress.stream()
                .map(progress -> {
                    double score = progress.getCompletion() * 100;

                    return LeaderboardEntry.builder()
                            .userId(progress.getUserId())
                            .score(score)
                            .averageCompletion(progress.getCompletion())
                            .completedCourses(progress.getCompletion() >= 1.0f ? 1 : 0)
                            .build();
                })
                .sorted(Comparator.comparing(LeaderboardEntry::getScore).reversed())
                .limit(limit != null ? limit : 100)
                .collect(Collectors.toList());

        // Set ranks
        for (int i = 0; i < entries.size(); i++) {
            entries.get(i).setRank(i + 1);
        }

        return LeaderboardResponse.builder()
                .type(LeaderboardType.PATH)
                .pathId(pathId)
                .pathTitle(path.getTitle())
                .entries(entries)
                .lastUpdated(LocalDateTime.now())
                .build();
    }

    @Override
    @Transactional
    public void updateLeaderboard(LeaderboardType type, UUID referenceId) {
        log.info("Updating leaderboard: type={}, referenceId={}", type, referenceId);
        // TODO: Implement periodic leaderboard update
        log.info("Leaderboard updated successfully");
    }

    @Override
    @Transactional(readOnly = true)
    public Integer getUserRank(UUID userId, LeaderboardType type, UUID referenceId) {
        log.debug("Getting rank for user {} in leaderboard: type={}, referenceId={}", userId, type, referenceId);

        LeaderboardResponse leaderboard = getLeaderboard(type, referenceId, null);

        return leaderboard.getEntries().stream()
                .filter(entry -> entry.getUserId().equals(userId))
                .map(LeaderboardEntry::getRank)
                .findFirst()
                .orElse(null);
    }
}
