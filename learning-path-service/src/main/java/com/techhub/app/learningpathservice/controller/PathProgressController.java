package com.techhub.app.learningpathservice.controller;

import com.techhub.app.commonservice.payload.GlobalResponse;
import com.techhub.app.learningpathservice.dto.request.UpdatePathProgressRequest;
import com.techhub.app.learningpathservice.dto.response.CompletionPercentageResponse;
import com.techhub.app.learningpathservice.dto.response.CompletionStatusResponse;
import com.techhub.app.learningpathservice.dto.response.PathProgressResponse;
import com.techhub.app.learningpathservice.service.PathProgressService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import java.util.List;
import java.util.UUID;

/**
 * Controller for Learning Path Progress management
 * Handles user progress tracking and updates
 */
@RestController
@RequestMapping("/api/v1/progress")
@RequiredArgsConstructor
@Slf4j
@Validated
public class PathProgressController {

    private final PathProgressService pathProgressService;

    /**
     * Start learning a path
     * POST /api/v1/progress/start
     */
    @PostMapping("/start")
    public ResponseEntity<GlobalResponse<PathProgressResponse>> startLearningPath(
            @RequestParam UUID userId,
            @RequestParam UUID pathId,
            HttpServletRequest httpRequest) {

        log.info("User {} starting learning path {}", userId, pathId);

        PathProgressResponse response = pathProgressService.startLearningPath(userId, pathId);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(GlobalResponse.success("Learning path started successfully", response)
                        .withStatus("LEARNING_PATH_STARTED")
                        .withPath(httpRequest.getRequestURI()));
    }

    /**
     * Update learning path progress
     * PUT /api/v1/progress/{pathId}
     */
    @PutMapping("/{pathId}")
    public ResponseEntity<GlobalResponse<PathProgressResponse>> updateProgress(
            @PathVariable UUID pathId,
            @Valid @RequestBody UpdatePathProgressRequest request,
            HttpServletRequest httpRequest) {

        log.info("Updating progress for user {} on path {}", request.getUserId(), pathId);

        // Ensure pathId matches the request
        request.setPathId(pathId);

        PathProgressResponse response = pathProgressService.updateProgress(request);

        return ResponseEntity.ok(
                GlobalResponse.success("Progress updated successfully", response)
                        .withStatus("PROGRESS_UPDATED")
                        .withPath(httpRequest.getRequestURI()));
    }

    /**
     * Get user progress for a specific learning path
     * GET /api/v1/progress/{pathId}/user/{userId}
     */
    @GetMapping("/{pathId}/user/{userId}")
    public ResponseEntity<GlobalResponse<PathProgressResponse>> getUserProgress(
            @PathVariable UUID pathId,
            @PathVariable UUID userId,
            HttpServletRequest httpRequest) {

        log.info("Retrieving progress for user {} on path {}", userId, pathId);

        PathProgressResponse response = pathProgressService.getProgressByUserAndPath(userId, pathId);

        return ResponseEntity.ok(
                GlobalResponse.success("Progress retrieved successfully", response)
                        .withPath(httpRequest.getRequestURI()));
    }

    /**
     * Get all progress for a user across all learning paths
     * GET /api/v1/progress/user/{userId}
     */
    @GetMapping("/user/{userId}")
    public ResponseEntity<GlobalResponse<List<PathProgressResponse>>> getAllUserProgress(
            @PathVariable UUID userId,
            HttpServletRequest httpRequest) {

        log.info("Retrieving all progress for user {}", userId);

        List<PathProgressResponse> response = pathProgressService.getAllProgressByUser(userId);

        return ResponseEntity.ok(
                GlobalResponse.success("User progress retrieved successfully", response)
                        .withPath(httpRequest.getRequestURI()));
    }

    /**
     * Get all users learning a specific path
     * GET /api/v1/progress/path/{pathId}
     */
    @GetMapping("/path/{pathId}")
    public ResponseEntity<GlobalResponse<List<PathProgressResponse>>> getPathProgress(
            @PathVariable UUID pathId,
            HttpServletRequest httpRequest) {

        log.info("Retrieving all users progress for path {}", pathId);

        List<PathProgressResponse> response = pathProgressService.getAllProgressByPath(pathId);

        return ResponseEntity.ok(
                GlobalResponse.success("Path progress retrieved successfully", response)
                        .withPath(httpRequest.getRequestURI()));
    }

    /**
     * Check if user completed a learning path
     * GET /api/v1/progress/{pathId}/user/{userId}/completed
     */
    @GetMapping("/{pathId}/user/{userId}/completed")
    public ResponseEntity<GlobalResponse<CompletionStatusResponse>> checkCompletion(
            @PathVariable UUID pathId,
            @PathVariable UUID userId,
            HttpServletRequest httpRequest) {

        log.info("Checking completion status for user {} on path {}", userId, pathId);

        Boolean isCompleted = pathProgressService.isPathCompleted(userId, pathId);

        CompletionStatusResponse response = CompletionStatusResponse.builder()
                .userId(userId)
                .pathId(pathId)
                .isCompleted(isCompleted)
                .build();

        return ResponseEntity.ok(
                GlobalResponse.success("Completion status retrieved successfully", response)
                        .withPath(httpRequest.getRequestURI()));
    }

    /**
     * Get all users who completed a specific path
     * GET /api/v1/progress/path/{pathId}/completed
     */
    @GetMapping("/path/{pathId}/completed")
    public ResponseEntity<GlobalResponse<List<PathProgressResponse>>> getCompletedUsers(
            @PathVariable UUID pathId,
            HttpServletRequest httpRequest) {

        log.info("Retrieving users who completed path {}", pathId);

        List<PathProgressResponse> response = pathProgressService.getCompletedUsers(pathId);

        return ResponseEntity.ok(
                GlobalResponse.success("Completed users retrieved successfully", response)
                        .withPath(httpRequest.getRequestURI()));
    }

    /**
     * Calculate completion percentage for a user on a path
     * GET /api/v1/progress/{pathId}/user/{userId}/completion
     */
    @GetMapping("/{pathId}/user/{userId}/completion")
    public ResponseEntity<GlobalResponse<CompletionPercentageResponse>> calculateCompletion(
            @PathVariable UUID pathId,
            @PathVariable UUID userId,
            HttpServletRequest httpRequest) {

        log.info("Calculating completion for user {} on path {}", userId, pathId);

        Float completion = pathProgressService.calculateCompletion(userId, pathId);

        CompletionPercentageResponse response = CompletionPercentageResponse.builder()
                .userId(userId)
                .pathId(pathId)
                .completionPercentage(completion)
                .build();

        return ResponseEntity.ok(
                GlobalResponse.success("Completion calculated successfully", response)
                        .withPath(httpRequest.getRequestURI()));
    }
}
