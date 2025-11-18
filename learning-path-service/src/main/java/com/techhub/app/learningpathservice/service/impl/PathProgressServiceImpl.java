package com.techhub.app.learningpathservice.service.impl;

import com.techhub.app.learningpathservice.dto.PathProgressResponseDTO;
import com.techhub.app.learningpathservice.dto.UpdateProgressRequestDTO;
import com.techhub.app.learningpathservice.entity.PathProgress;
import com.techhub.app.learningpathservice.mapper.PathProgressMapper;
import com.techhub.app.learningpathservice.repository.PathProgressRepository;
import com.techhub.app.learningpathservice.service.PathProgressService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class PathProgressServiceImpl implements PathProgressService {

    private final PathProgressRepository pathProgressRepository;
    private final PathProgressMapper pathProgressMapper;

    @Override
    public PathProgressResponseDTO createOrUpdateProgress(UpdateProgressRequestDTO requestDTO) {
        log.info("Creating or updating progress for user {} on path {}",
                requestDTO.getUserId(), requestDTO.getPathId());

        Optional<PathProgress> existingProgress = pathProgressRepository
                .findByUserIdAndPathIdAndIsActive(
                        requestDTO.getUserId(),
                        requestDTO.getPathId(),
                        Boolean.TRUE);

        PathProgress pathProgress;
        if (existingProgress.isPresent()) {
            pathProgress = existingProgress.get();
            pathProgressMapper.updateEntity(pathProgress, requestDTO);
            log.info("Updating existing progress with ID: {}", pathProgress.getId());
        } else {
            pathProgress = pathProgressMapper.toEntity(requestDTO);
            log.info("Creating new progress");
        }

        pathProgress = pathProgressRepository.save(pathProgress);

        log.info("Progress saved successfully with ID: {}", pathProgress.getId());
        return pathProgressMapper.toDTO(pathProgress);
    }

    @Override
    @Transactional(readOnly = true)
    public PathProgressResponseDTO getProgressByUserAndPath(UUID userId, UUID pathId) {
        log.info("Fetching progress for user {} on path {}", userId, pathId);

        PathProgress pathProgress = pathProgressRepository
                .findByUserIdAndPathIdAndIsActive(userId, pathId, Boolean.TRUE)
                .orElseThrow(() -> new RuntimeException(
                        "Progress not found for user " + userId + " on path " + pathId));

        return pathProgressMapper.toDTO(pathProgress);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<PathProgressResponseDTO> getProgressByUser(UUID userId, Pageable pageable) {
        log.info("Fetching all progress for user {}", userId);

        Page<PathProgress> progressList = pathProgressRepository
                .findByUserIdAndIsActive(userId, Boolean.TRUE, pageable);

        return progressList.map(pathProgressMapper::toDTO);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<PathProgressResponseDTO> getProgressByPath(UUID pathId, Pageable pageable) {
        log.info("Fetching all progress for path {}", pathId);

        Page<PathProgress> progressList = pathProgressRepository
                .findByPathIdAndIsActive(pathId, Boolean.TRUE, pageable);

        return progressList.map(pathProgressMapper::toDTO);
    }

    @Override
    public void deleteProgress(UUID userId, UUID pathId) {
        log.info("Deleting progress for user {} on path {}", userId, pathId);

        PathProgress pathProgress = pathProgressRepository
                .findByUserIdAndPathIdAndIsActive(userId, pathId, Boolean.TRUE)
                .orElseThrow(() -> new RuntimeException(
                        "Progress not found for user " + userId + " on path " + pathId));

        pathProgress.setIsActive(Boolean.FALSE);
        pathProgressRepository.save(pathProgress);

        log.info("Progress deleted successfully");
    }

    @Override
    @Transactional(readOnly = true)
    public Long countEnrolledUsers(UUID pathId) {
        log.info("Counting enrolled users for path {}", pathId);
        return pathProgressRepository.countByPathId(pathId);
    }

    @Override
    @Transactional(readOnly = true)
    public Float getAverageCompletion(UUID pathId) {
        log.info("Calculating average completion for path {}", pathId);
        Float average = pathProgressRepository.getAverageCompletionByPathId(pathId);
        return average != null ? average : 0.0f;
    }
}
