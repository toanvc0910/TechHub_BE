package com.techhub.app.learningpathservice.service.impl;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.techhub.app.learningpathservice.dto.request.UpdatePathProgressRequest;
import com.techhub.app.learningpathservice.dto.response.PathProgressResponse;
import com.techhub.app.learningpathservice.entity.LearningPath;
import com.techhub.app.learningpathservice.entity.PathProgress;
import com.techhub.app.learningpathservice.exception.ResourceNotFoundException;
import com.techhub.app.learningpathservice.mapper.PathProgressMapper;
import com.techhub.app.learningpathservice.repository.LearningPathRepository;
import com.techhub.app.learningpathservice.repository.PathProgressRepository;
import com.techhub.app.learningpathservice.service.PathProgressService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class PathProgressServiceImpl implements PathProgressService {

    private final PathProgressRepository progressRepository;
    private final LearningPathRepository learningPathRepository;
    private final PathProgressMapper mapper;

    @Override
    @Transactional
    public PathProgressResponse startLearningPath(UUID userId, UUID pathId) {
        log.info("User {} starting learning path {}", userId, pathId);

        // Verify path exists
        LearningPath path = learningPathRepository.findById(pathId)
                .orElseThrow(() -> new ResourceNotFoundException("Learning path not found with ID: " + pathId));

        // Check if already started
        if (progressRepository.findByUserIdAndPathIdAndIsActive(userId, pathId, "Y").isPresent()) {
            throw new IllegalStateException("User already started this learning path");
        }

        // Create new progress
        PathProgress progress = new PathProgress();
        progress.setUserId(userId);
        progress.setPathId(pathId);
        progress.setCompletion(0.0f);
        progress.setMilestones(new ArrayList<>());
        progress.setCreated(LocalDateTime.now());
        progress.setUpdated(LocalDateTime.now());
        progress.setCreatedBy(userId);
        progress.setIsActive("Y");

        PathProgress saved = progressRepository.save(progress);

        PathProgressResponse response = mapper.toResponse(saved);
        response.setPathTitle(path.getTitle());

        log.info("User {} started learning path {} successfully", userId, pathId);
        return response;
    }

    @Override
    @Transactional
    public PathProgressResponse updateProgress(UpdatePathProgressRequest request) {
        log.info("Updating progress for user {} on path {}", request.getUserId(), request.getPathId());

        PathProgress progress = progressRepository
                .findByUserIdAndPathIdAndIsActive(request.getUserId(), request.getPathId(), "Y")
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Progress not found for user " + request.getUserId() + " on path " + request.getPathId()));

        // Update fields
        if (request.getCompletion() != null) {
            progress.setCompletion(request.getCompletion());
        }

        if (request.getMilestones() != null) {
            // Convert Map to List<String> for JSONB storage
            List<String> milestonesList = new ArrayList<>();
            request.getMilestones().forEach((key, value) -> {
                milestonesList.add(key + ":" + value);
            });
            progress.setMilestones(milestonesList);
        }

        progress.setUpdated(LocalDateTime.now());
        progress.setUpdatedBy(request.getUserId());

        PathProgress updated = progressRepository.save(progress);

        PathProgressResponse response = mapper.toResponse(updated);
        enrichResponse(response);

        log.info("Updated progress successfully");
        return response;
    }

    @Override
    @Transactional(readOnly = true)
    public PathProgressResponse getProgressByUserAndPath(UUID userId, UUID pathId) {
        log.debug("Getting progress for user {} on path {}", userId, pathId);

        PathProgress progress = progressRepository
                .findByUserIdAndPathIdAndIsActive(userId, pathId, "Y")
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Progress not found for user " + userId + " on path " + pathId));

        PathProgressResponse response = mapper.toResponse(progress);
        enrichResponse(response);

        return response;
    }

    @Override
    @Transactional(readOnly = true)
    public List<PathProgressResponse> getAllProgressByUser(UUID userId) {
        log.debug("Getting all progress for user {}", userId);

        List<PathProgress> progressList = progressRepository.findByUserIdAndIsActive(userId, "Y");
        List<PathProgressResponse> responses = mapper.toResponseList(progressList);

        responses.forEach(this::enrichResponse);

        return responses;
    }

    @Override
    @Transactional(readOnly = true)
    public List<PathProgressResponse> getAllProgressByPath(UUID pathId) {
        log.debug("Getting all progress for path {}", pathId);

        List<PathProgress> progressList = progressRepository.findByPathIdAndIsActive(pathId, "Y");
        List<PathProgressResponse> responses = mapper.toResponseList(progressList);

        responses.forEach(this::enrichResponse);

        return responses;
    }

    @Override
    @Transactional(readOnly = true)
    public Boolean isPathCompleted(UUID userId, UUID pathId) {
        return progressRepository.findByUserIdAndPathIdAndIsActive(userId, pathId, "Y")
                .map(progress -> progress.getCompletion() >= 1.0f)
                .orElse(false);
    }

    @Override
    @Transactional(readOnly = true)
    public List<PathProgressResponse> getCompletedUsers(UUID pathId) {
        log.debug("Getting completed users for path {}", pathId);

        List<PathProgress> progressList = progressRepository
                .findByPathIdAndCompletionGreaterThanEqualAndIsActive(pathId, 1.0f, "Y");

        List<PathProgressResponse> responses = mapper.toResponseList(progressList);
        responses.forEach(this::enrichResponse);

        return responses;
    }

    @Override
    @Transactional(readOnly = true)
    public Float calculateCompletion(UUID userId, UUID pathId) {
        log.debug("Calculating completion for user {} on path {}", userId, pathId);

        // TODO: Calculate based on completed courses from Course Service
        return progressRepository.findByUserIdAndPathIdAndIsActive(userId, pathId, "Y")
                .map(PathProgress::getCompletion)
                .orElse(0.0f);
    }

    /**
     * Enrich response with path title
     */
    private void enrichResponse(PathProgressResponse response) {
        if (response.getPathId() != null) {
            learningPathRepository.findById(response.getPathId())
                    .ifPresent(path -> response.setPathTitle(path.getTitle()));
        }
    }
}
