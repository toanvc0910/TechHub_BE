package com.techhub.app.learningpathservice.service;

import com.techhub.app.learningpathservice.dto.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.UUID;

public interface LearningPathService {

    LearningPathResponseDTO createLearningPath(LearningPathRequestDTO requestDTO);

    LearningPathResponseDTO updateLearningPath(UUID id, LearningPathRequestDTO requestDTO, UUID requesterId,
            boolean isAdmin);

    LearningPathResponseDTO getLearningPathById(UUID id);

    Page<LearningPathResponseDTO> getAllLearningPaths(Pageable pageable);

    /**
     * Management listing scoped to the current user: admins see every author's
     * paths, while other roles (e.g. INSTRUCTOR) see only the ones they created.
     */
    Page<LearningPathResponseDTO> getMyLearningPaths(UUID requesterId, boolean isAdmin, Pageable pageable);

    Page<LearningPathResponseDTO> searchLearningPaths(String keyword, Pageable pageable);

    Page<LearningPathResponseDTO> getLearningPathsByCreator(UUID userId, Pageable pageable);

    void deleteLearningPath(UUID id, UUID requesterId, boolean isAdmin);

    LearningPathResponseDTO addCoursesToPath(UUID pathId, AddCoursesToPathRequestDTO requestDTO);

    LearningPathResponseDTO removeCourseFromPath(UUID pathId, UUID courseId);

    LearningPathResponseDTO reorderCourses(UUID pathId, List<CourseInPathDTO> courses);

    List<LearningPathResponseDTO> getLearningPathsByCourse(UUID courseId);
}
