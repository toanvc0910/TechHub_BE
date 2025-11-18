package com.techhub.app.learningpathservice.service;

import java.util.UUID;

import com.techhub.app.learningpathservice.dto.response.LeaderboardResponse;
import com.techhub.app.learningpathservice.enums.LeaderboardType;

public interface LeaderboardService {

    /**
     * Lấy leaderboard theo loại
     */
    LeaderboardResponse getLeaderboard(LeaderboardType type, UUID referenceId, Integer limit);

    /**
     * Lấy global leaderboard
     */
    LeaderboardResponse getGlobalLeaderboard(Integer limit);

    /**
     * Lấy leaderboard của một learning path
     */
    LeaderboardResponse getPathLeaderboard(UUID pathId, Integer limit);

    /**
     * Cập nhật leaderboard (gọi định kỳ hoặc khi có thay đổi)
     */
    void updateLeaderboard(LeaderboardType type, UUID referenceId);

    /**
     * Lấy rank của một user trong leaderboard
     */
    Integer getUserRank(UUID userId, LeaderboardType type, UUID referenceId);
}