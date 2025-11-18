package com.techhub.app.learningpathservice.controller;

import com.techhub.app.commonservice.payload.GlobalResponse;
import com.techhub.app.learningpathservice.dto.response.LeaderboardResponse;
import com.techhub.app.learningpathservice.dto.response.UserRankResponse;
import com.techhub.app.learningpathservice.enums.LeaderboardType;
import com.techhub.app.learningpathservice.service.LeaderboardService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.UUID;

/**
 * Controller for Leaderboard management
 * Handles leaderboard retrieval and rankings
 */
@RestController
@RequestMapping("/api/v1/leaderboard")
@RequiredArgsConstructor
@Slf4j
@Validated
public class LeaderboardController {

    private final LeaderboardService leaderboardService;

    /**
     * Get global leaderboard
     * GET /api/v1/leaderboard/global
     */
    @GetMapping("/global")
    public ResponseEntity<GlobalResponse<LeaderboardResponse>> getGlobalLeaderboard(
            @RequestParam(required = false, defaultValue = "20") Integer limit,
            HttpServletRequest httpRequest) {

        log.info("Retrieving global leaderboard with limit {}", limit);

        LeaderboardResponse response = leaderboardService.getGlobalLeaderboard(limit);

        return ResponseEntity.ok(
                GlobalResponse.success("Global leaderboard retrieved successfully", response)
                        .withPath(httpRequest.getRequestURI()));
    }

    /**
     * Get leaderboard for a specific learning path
     * GET /api/v1/leaderboard/path/{pathId}
     */
    @GetMapping("/path/{pathId}")
    public ResponseEntity<GlobalResponse<LeaderboardResponse>> getPathLeaderboard(
            @PathVariable UUID pathId,
            @RequestParam(required = false, defaultValue = "20") Integer limit,
            HttpServletRequest httpRequest) {

        log.info("Retrieving leaderboard for path {} with limit {}", pathId, limit);

        LeaderboardResponse response = leaderboardService.getPathLeaderboard(pathId, limit);

        return ResponseEntity.ok(
                GlobalResponse.success("Path leaderboard retrieved successfully", response)
                        .withPath(httpRequest.getRequestURI()));
    }

    /**
     * Get leaderboard by type
     * GET /api/v1/leaderboard
     */
    @GetMapping
    public ResponseEntity<GlobalResponse<LeaderboardResponse>> getLeaderboard(
            @RequestParam LeaderboardType type,
            @RequestParam(required = false) UUID referenceId,
            @RequestParam(required = false, defaultValue = "20") Integer limit,
            HttpServletRequest httpRequest) {

        log.info("Retrieving leaderboard of type {} with referenceId {} and limit {}",
                type, referenceId, limit);

        LeaderboardResponse response = leaderboardService.getLeaderboard(type, referenceId, limit);

        return ResponseEntity.ok(
                GlobalResponse.success("Leaderboard retrieved successfully", response)
                        .withPath(httpRequest.getRequestURI()));
    }

    /**
     * Get user rank in a leaderboard
     * GET /api/v1/leaderboard/rank/{userId}
     */
    @GetMapping("/rank/{userId}")
    public ResponseEntity<GlobalResponse<UserRankResponse>> getUserRank(
            @PathVariable UUID userId,
            @RequestParam LeaderboardType type,
            @RequestParam(required = false) UUID referenceId,
            HttpServletRequest httpRequest) {

        log.info("Retrieving rank for user {} in leaderboard type {} with referenceId {}",
                userId, type, referenceId);

        Integer rank = leaderboardService.getUserRank(userId, type, referenceId);

        UserRankResponse response = UserRankResponse.builder()
                .userId(userId)
                .leaderboardType(type)
                .referenceId(referenceId)
                .rank(rank)
                .build();

        return ResponseEntity.ok(
                GlobalResponse.success("User rank retrieved successfully", response)
                        .withPath(httpRequest.getRequestURI()));
    }

    /**
     * Update/Refresh leaderboard
     * POST /api/v1/leaderboard/refresh
     */
    @PostMapping("/refresh")
    public ResponseEntity<GlobalResponse<Void>> refreshLeaderboard(
            @RequestParam LeaderboardType type,
            @RequestParam(required = false) UUID referenceId,
            HttpServletRequest httpRequest) {

        log.info("Refreshing leaderboard of type {} with referenceId {}", type, referenceId);

        leaderboardService.updateLeaderboard(type, referenceId);

        return ResponseEntity.ok(
                GlobalResponse.<Void>success("Leaderboard refreshed successfully", null)
                        .withStatus("LEADERBOARD_REFRESHED")
                        .withPath(httpRequest.getRequestURI()));
    }
}
