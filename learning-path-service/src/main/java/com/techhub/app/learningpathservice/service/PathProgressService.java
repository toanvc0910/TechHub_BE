package com.techhub.app.learningpathservice.service;

import com.techhub.app.learningpathservice.dto.PathProgressResponseDTO;
import com.techhub.app.learningpathservice.dto.UpdateProgressRequestDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface PathProgressService {

    PathProgressResponseDTO createOrUpdateProgress(UpdateProgressRequestDTO requestDTO);

    PathProgressResponseDTO getProgressByUserAndPath(UUID userId, UUID pathId);

    Page<PathProgressResponseDTO> getProgressByUser(UUID userId, Pageable pageable);

    Page<PathProgressResponseDTO> getProgressByPath(UUID pathId, Pageable pageable);

    void deleteProgress(UUID userId, UUID pathId);

    Long countEnrolledUsers(UUID pathId);

    Float getAverageCompletion(UUID pathId);
}
