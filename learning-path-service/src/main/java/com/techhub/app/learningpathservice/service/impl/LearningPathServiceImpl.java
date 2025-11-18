package com.techhub.app.learningpathservice.service.impl;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.techhub.app.learningpathservice.dto.request.CourseOrderRequest;
import com.techhub.app.learningpathservice.dto.request.CreateLearningPathRequest;
import com.techhub.app.learningpathservice.dto.request.UpdateLearningPathRequest;
import com.techhub.app.learningpathservice.dto.response.LearningPathResponse;
import com.techhub.app.learningpathservice.entity.LearningPath;
import com.techhub.app.learningpathservice.entity.LearningPathCourse;
import com.techhub.app.learningpathservice.exception.ResourceNotFoundException;
import com.techhub.app.learningpathservice.mapper.LearningPathMapper;
import com.techhub.app.learningpathservice.repository.LearningPathCourseRepository;
import com.techhub.app.learningpathservice.repository.LearningPathRepository;
import com.techhub.app.learningpathservice.repository.PathProgressRepository;
import com.techhub.app.learningpathservice.service.LearningPathService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class LearningPathServiceImpl implements LearningPathService {

    private final LearningPathRepository learningPathRepository;
    private final LearningPathCourseRepository pathCourseRepository;
    private final PathProgressRepository pathProgressRepository;
    private final LearningPathMapper mapper;

    @Override
    @Transactional
    public LearningPathResponse createLearningPath(CreateLearningPathRequest request, UUID createdBy) {
        log.info("Creating learning path: {}", request.getTitle());

        // Convert DTO to Entity
        LearningPath entity = mapper.toEntity(request);
        entity.setCreatedBy(createdBy);
        entity.setUpdatedBy(createdBy);

        // Save learning path
        LearningPath saved = learningPathRepository.save(entity);

        // Add courses if provided
        if (request.getCourses() != null && !request.getCourses().isEmpty()) {
            for (CourseOrderRequest courseRequest : request.getCourses()) {
                LearningPathCourse pathCourse = new LearningPathCourse();
                pathCourse.setPathId(saved.getId());
                pathCourse.setCourseId(courseRequest.getCourseId());
                pathCourse.setOrder(courseRequest.getOrder());
                pathCourseRepository.save(pathCourse);
            }
        }

        // Convert to response
        LearningPathResponse response = mapper.toResponse(saved);
        enrichResponse(response);

        log.info("Created learning path with ID: {}", saved.getId());
        return response;
    }

    @Override
    @Transactional
    public LearningPathResponse updateLearningPath(UUID id, UpdateLearningPathRequest request, UUID updatedBy) {
        log.info("Updating learning path: {}", id);

        LearningPath entity = learningPathRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Learning path not found with ID: " + id));

        // Update fields
        mapper.updateEntity(request, entity);
        entity.setUpdatedBy(updatedBy);
        entity.setUpdated(LocalDateTime.now());

        LearningPath updated = learningPathRepository.save(entity);

        LearningPathResponse response = mapper.toResponse(updated);
        enrichResponse(response);

        log.info("Updated learning path: {}", id);
        return response;
    }

    @Override
    @Transactional
    public void deleteLearningPath(UUID id, UUID deletedBy) {
        log.info("Deleting learning path: {}", id);

        LearningPath entity = learningPathRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Learning path not found with ID: " + id));

        // Soft delete
        entity.setIsActive("N");
        entity.setUpdatedBy(deletedBy);
        entity.setUpdated(LocalDateTime.now());

        learningPathRepository.save(entity);

        log.info("Deleted learning path: {}", id);
    }

    @Override
    @Transactional(readOnly = true)
    public LearningPathResponse getLearningPathById(UUID id) {
        log.debug("Getting learning path: {}", id);

        LearningPath entity = learningPathRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Learning path not found with ID: " + id));

        LearningPathResponse response = mapper.toResponse(entity);
        enrichResponse(response);

        return response;
    }

    @Override
    @Transactional(readOnly = true)
    public List<LearningPathResponse> getAllLearningPaths() {
        log.debug("Getting all learning paths");

        List<LearningPath> entities = learningPathRepository.findAll();
        List<LearningPathResponse> responses = mapper.toResponseList(entities);

        responses.forEach(this::enrichResponse);

        return responses;
    }

    @Override
    @Transactional(readOnly = true)
    public List<LearningPathResponse> getActiveLearningPaths() {
        log.debug("Getting active learning paths");

        List<LearningPath> entities = learningPathRepository.findByIsActive("Y");
        List<LearningPathResponse> responses = mapper.toResponseList(entities);

        responses.forEach(this::enrichResponse);

        return responses;
    }

    @Override
    @Transactional(readOnly = true)
    public List<LearningPathResponse> searchLearningPaths(String keyword) {
        log.debug("Searching learning paths with keyword: {}", keyword);

        List<LearningPath> entities = learningPathRepository.searchByTitle(keyword);
        List<LearningPathResponse> responses = mapper.toResponseList(entities);

        responses.forEach(this::enrichResponse);

        return responses;
    }

    @Override
    @Transactional
    public void addCourseToPath(UUID pathId, CourseOrderRequest request, UUID updatedBy) {
        log.info("Adding course {} to path {}", request.getCourseId(), pathId);

        // Verify path exists
        LearningPath path = learningPathRepository.findById(pathId)
                .orElseThrow(() -> new ResourceNotFoundException("Learning path not found with ID: " + pathId));

        // Check if course already in path
        if (pathCourseRepository.existsByPathIdAndCourseId(pathId, request.getCourseId())) {
            throw new IllegalArgumentException("Course already exists in this path");
        }

        // Add course
        LearningPathCourse pathCourse = new LearningPathCourse();
        pathCourse.setPathId(pathId);
        pathCourse.setCourseId(request.getCourseId());
        pathCourse.setOrder(request.getOrder());

        pathCourseRepository.save(pathCourse);

        // Update path timestamp
        path.setUpdated(LocalDateTime.now());
        path.setUpdatedBy(updatedBy);
        learningPathRepository.save(path);

        log.info("Added course to path successfully");
    }

    @Override
    @Transactional
    public void removeCourseFromPath(UUID pathId, UUID courseId, UUID updatedBy) {
        log.info("Removing course {} from path {}", courseId, pathId);

        // Verify path exists
        LearningPath path = learningPathRepository.findById(pathId)
                .orElseThrow(() -> new ResourceNotFoundException("Learning path not found with ID: " + pathId));

        // Remove course
        pathCourseRepository.removeFromPath(pathId, courseId);

        // Update path timestamp
        path.setUpdated(LocalDateTime.now());
        path.setUpdatedBy(updatedBy);
        learningPathRepository.save(path);

        log.info("Removed course from path successfully");
    }

    @Override
    @Transactional
    public void updateCourseOrder(UUID pathId, List<CourseOrderRequest> courses, UUID updatedBy) {
        log.info("Updating course order for path {}", pathId);

        // Verify path exists
        LearningPath path = learningPathRepository.findById(pathId)
                .orElseThrow(() -> new ResourceNotFoundException("Learning path not found with ID: " + pathId));

        // Get existing courses
        List<LearningPathCourse> existingCourses = pathCourseRepository.findByPathIdOrderByOrderAsc(pathId);

        // Update order
        for (CourseOrderRequest request : courses) {
            existingCourses.stream()
                    .filter(pc -> pc.getCourseId().equals(request.getCourseId()))
                    .findFirst()
                    .ifPresent(pc -> pc.setOrder(request.getOrder()));
        }

        pathCourseRepository.saveAll(existingCourses);

        // Update path timestamp
        path.setUpdated(LocalDateTime.now());
        path.setUpdatedBy(updatedBy);
        learningPathRepository.save(path);

        log.info("Updated course order successfully");
    }

    @Override
    @Transactional(readOnly = true)
    public Integer getTotalEnrolled(UUID pathId) {
        return (int) pathProgressRepository.countByPathIdAndIsActive(pathId, "Y");
    }

    @Override
    @Transactional(readOnly = true)
    public Double getAverageCompletion(UUID pathId) {
        Double avg = pathProgressRepository.getAverageCompletion(pathId);
        return avg != null ? avg : 0.0;
    }

    /**
     * Enrich response with additional data
     */
    private void enrichResponse(LearningPathResponse response) {
        if (response.getId() != null) {
            // Get course count
            long courseCount = pathCourseRepository.countByPathId(response.getId());
            response.setTotalCourses((int) courseCount);

            // Get enrollment count
            response.setTotalEnrolled(getTotalEnrolled(response.getId()));

            // Get average completion
            response.setAverageCompletion(getAverageCompletion(response.getId()));

            // TODO: Fetch course details from Course Service
            // response.setCourses(fetchCourseDetails(response.getId()));
        }
    }
}
