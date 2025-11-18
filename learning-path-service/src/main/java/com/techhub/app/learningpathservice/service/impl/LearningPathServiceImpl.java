package com.techhub.app.learningpathservice.service.impl;

import com.techhub.app.learningpathservice.dto.*;
import com.techhub.app.learningpathservice.entity.LearningPath;
import com.techhub.app.learningpathservice.entity.LearningPathCourse;
import com.techhub.app.learningpathservice.mapper.LearningPathMapper;
import com.techhub.app.learningpathservice.repository.LearningPathCourseRepository;
import com.techhub.app.learningpathservice.repository.LearningPathRepository;
import com.techhub.app.learningpathservice.service.LearningPathService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class LearningPathServiceImpl implements LearningPathService {

    private final LearningPathRepository learningPathRepository;
    private final LearningPathCourseRepository learningPathCourseRepository;
    private final LearningPathMapper learningPathMapper;

    @Override
    public LearningPathResponseDTO createLearningPath(LearningPathRequestDTO requestDTO) {
        log.info("Creating new learning path with title: {}", requestDTO.getTitle());

        LearningPath learningPath = learningPathMapper.toEntity(requestDTO);
        learningPath = learningPathRepository.save(learningPath);

        log.info("Learning path created successfully with ID: {}", learningPath.getId());
        return learningPathMapper.toDTO(learningPath);
    }

    @Override
    public LearningPathResponseDTO updateLearningPath(UUID id, LearningPathRequestDTO requestDTO) {
        log.info("Updating learning path with ID: {}", id);

        LearningPath learningPath = learningPathRepository.findByIdAndIsActive(id, Boolean.TRUE)
                .orElseThrow(() -> new RuntimeException("Learning path not found with ID: " + id));

        learningPathMapper.updateEntity(learningPath, requestDTO);
        learningPath = learningPathRepository.save(learningPath);

        log.info("Learning path updated successfully with ID: {}", id);
        return learningPathMapper.toDTO(learningPath);
    }

    @Override
    @Transactional(readOnly = true)
    public LearningPathResponseDTO getLearningPathById(UUID id) {
        log.info("Fetching learning path with ID: {}", id);

        LearningPath learningPath = learningPathRepository.findByIdAndIsActive(id, Boolean.TRUE)
                .orElseThrow(() -> new RuntimeException("Learning path not found with ID: " + id));

        return learningPathMapper.toDTO(learningPath);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<LearningPathResponseDTO> getAllLearningPaths(Pageable pageable) {
        log.info("Fetching all learning paths");

        Page<LearningPath> learningPaths = learningPathRepository.findByIsActive(Boolean.TRUE, pageable);
        return learningPaths.map(learningPathMapper::toDTO);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<LearningPathResponseDTO> searchLearningPaths(String keyword, Pageable pageable) {
        log.info("Searching learning paths with keyword: {}", keyword);

        Page<LearningPath> learningPaths = learningPathRepository.searchLearningPaths(keyword, pageable);
        return learningPaths.map(learningPathMapper::toDTO);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<LearningPathResponseDTO> getLearningPathsByCreator(UUID userId, Pageable pageable) {
        log.info("Fetching learning paths created by user ID: {}", userId);

        Page<LearningPath> learningPaths = learningPathRepository.findByCreatedBy(userId, pageable);
        return learningPaths.map(learningPathMapper::toDTO);
    }

    @Override
    public void deleteLearningPath(UUID id) {
        log.info("Deleting learning path with ID: {}", id);

        LearningPath learningPath = learningPathRepository.findByIdAndIsActive(id, Boolean.TRUE)
                .orElseThrow(() -> new RuntimeException("Learning path not found with ID: " + id));

        learningPath.setIsActive(Boolean.FALSE);
        learningPathRepository.save(learningPath);

        log.info("Learning path deleted successfully with ID: {}", id);
    }

    @Override
    public LearningPathResponseDTO addCoursesToPath(UUID pathId, AddCoursesToPathRequestDTO requestDTO) {
        log.info("Adding courses to learning path ID: {}", pathId);

        LearningPath learningPath = learningPathRepository.findByIdAndIsActive(pathId, Boolean.TRUE)
                .orElseThrow(() -> new RuntimeException("Learning path not found with ID: " + pathId));

        for (CourseInPathDTO courseDTO : requestDTO.getCourses()) {
            // Check if course already exists in path
            if (learningPathCourseRepository.existsByPathIdAndCourseId(pathId, courseDTO.getCourseId())) {
                log.warn("Course {} already exists in path {}", courseDTO.getCourseId(), pathId);
                continue;
            }

            LearningPathCourse pathCourse = new LearningPathCourse();
            pathCourse.setPathId(pathId);
            pathCourse.setCourseId(courseDTO.getCourseId());
            pathCourse.setOrder(courseDTO.getOrder());

            learningPathCourseRepository.save(pathCourse);
        }

        // Refresh to get updated courses
        learningPath = learningPathRepository.findById(pathId).orElseThrow();

        log.info("Courses added successfully to learning path ID: {}", pathId);
        return learningPathMapper.toDTO(learningPath);
    }

    @Override
    public LearningPathResponseDTO removeCourseFromPath(UUID pathId, UUID courseId) {
        log.info("Removing course {} from learning path {}", courseId, pathId);

        LearningPath learningPath = learningPathRepository.findByIdAndIsActive(pathId, Boolean.TRUE)
                .orElseThrow(() -> new RuntimeException("Learning path not found with ID: " + pathId));

        learningPathCourseRepository.deleteByPathIdAndCourseId(pathId, courseId);

        // Refresh to get updated courses
        learningPath = learningPathRepository.findById(pathId).orElseThrow();

        log.info("Course removed successfully from learning path ID: {}", pathId);
        return learningPathMapper.toDTO(learningPath);
    }

    @Override
    public LearningPathResponseDTO reorderCourses(UUID pathId, List<CourseInPathDTO> courses) {
        log.info("Reordering courses in learning path ID: {}", pathId);

        LearningPath learningPath = learningPathRepository.findByIdAndIsActive(pathId, Boolean.TRUE)
                .orElseThrow(() -> new RuntimeException("Learning path not found with ID: " + pathId));

        // Delete existing courses
        learningPathCourseRepository.deleteByPathId(pathId);

        // Add courses with new order
        for (CourseInPathDTO courseDTO : courses) {
            LearningPathCourse pathCourse = new LearningPathCourse();
            pathCourse.setPathId(pathId);
            pathCourse.setCourseId(courseDTO.getCourseId());
            pathCourse.setOrder(courseDTO.getOrder());
            pathCourse.setPositionX(courseDTO.getPositionX());
            pathCourse.setPositionY(courseDTO.getPositionY());
            pathCourse.setIsOptional(courseDTO.getIsOptional() != null ? courseDTO.getIsOptional() : "N");

            learningPathCourseRepository.save(pathCourse);
        }

        // Refresh to get updated courses
        learningPath = learningPathRepository.findById(pathId).orElseThrow();

        log.info("Courses reordered successfully in learning path ID: {}", pathId);
        return learningPathMapper.toDTO(learningPath);
    }

    @Override
    @Transactional(readOnly = true)
    public List<LearningPathResponseDTO> getLearningPathsByCourse(UUID courseId) {
        log.info("Fetching learning paths containing course ID: {}", courseId);

        List<LearningPathCourse> pathCourses = learningPathCourseRepository.findByCourseId(courseId);
        List<UUID> pathIds = pathCourses.stream()
                .map(LearningPathCourse::getPathId)
                .collect(Collectors.toList());

        List<LearningPath> learningPaths = learningPathRepository.findAllById(pathIds);
        return learningPathMapper.toDTOList(learningPaths);
    }
}
