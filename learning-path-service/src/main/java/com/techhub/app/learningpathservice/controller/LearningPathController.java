package com.techhub.app.learningpathservice.controller;

import com.techhub.app.commonservice.payload.GlobalResponse;
import com.techhub.app.learningpathservice.dto.request.CourseOrderRequest;
import com.techhub.app.learningpathservice.dto.request.CreateLearningPathRequest;
import com.techhub.app.learningpathservice.dto.request.UpdateLearningPathRequest;
import com.techhub.app.learningpathservice.dto.response.LearningPathResponse;
import com.techhub.app.learningpathservice.dto.response.LearningPathStatsResponse;
import com.techhub.app.learningpathservice.service.LearningPathService;
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
 * Controller for Learning Path management
 * Handles CRUD operations and course management within learning paths
 */
@RestController
@RequestMapping("/api/v1/learning-paths")
@RequiredArgsConstructor
@Slf4j
@Validated
public class LearningPathController {

    private final LearningPathService learningPathService;

    /**
     * Create a new learning path
     * POST /api/v1/learning-paths
     */
    @PostMapping
    public ResponseEntity<GlobalResponse<LearningPathResponse>> createLearningPath(
            @Valid @RequestBody CreateLearningPathRequest request,
            @RequestParam(required = false) UUID createdBy,
            HttpServletRequest httpRequest) {

        log.info("Creating new learning path: {}", request.getTitle());

        UUID creator = createdBy != null ? createdBy : UUID.randomUUID(); // TODO: Get from JWT
        LearningPathResponse response = learningPathService.createLearningPath(request, creator);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(GlobalResponse.success("Learning path created successfully", response)
                        .withStatus("LEARNING_PATH_CREATED")
                        .withPath(httpRequest.getRequestURI()));
    }

    /**
     * Update an existing learning path
     * PUT /api/v1/learning-paths/{pathId}
     */
    @PutMapping("/{pathId}")
    public ResponseEntity<GlobalResponse<LearningPathResponse>> updateLearningPath(
            @PathVariable UUID pathId,
            @Valid @RequestBody UpdateLearningPathRequest request,
            @RequestParam(required = false) UUID updatedBy,
            HttpServletRequest httpRequest) {

        log.info("Updating learning path: {}", pathId);

        UUID updater = updatedBy != null ? updatedBy : UUID.randomUUID(); // TODO: Get from JWT
        LearningPathResponse response = learningPathService.updateLearningPath(pathId, request, updater);

        return ResponseEntity.ok(
                GlobalResponse.success("Learning path updated successfully", response)
                        .withStatus("LEARNING_PATH_UPDATED")
                        .withPath(httpRequest.getRequestURI()));
    }

    /**
     * Delete a learning path (soft delete)
     * DELETE /api/v1/learning-paths/{pathId}
     */
    @DeleteMapping("/{pathId}")
    public ResponseEntity<GlobalResponse<Void>> deleteLearningPath(
            @PathVariable UUID pathId,
            @RequestParam(required = false) UUID deletedBy,
            HttpServletRequest httpRequest) {

        log.info("Deleting learning path: {}", pathId);

        UUID deleter = deletedBy != null ? deletedBy : UUID.randomUUID(); // TODO: Get from JWT
        learningPathService.deleteLearningPath(pathId, deleter);

        return ResponseEntity.ok(
                GlobalResponse.<Void>success("Learning path deleted successfully", null)
                        .withStatus("LEARNING_PATH_DELETED")
                        .withPath(httpRequest.getRequestURI()));
    }

    /**
     * Get a learning path by ID
     * GET /api/v1/learning-paths/{pathId}
     */
    @GetMapping("/{pathId}")
    public ResponseEntity<GlobalResponse<LearningPathResponse>> getLearningPath(
            @PathVariable UUID pathId,
            HttpServletRequest httpRequest) {

        log.info("Retrieving learning path: {}", pathId);

        LearningPathResponse response = learningPathService.getLearningPathById(pathId);

        return ResponseEntity.ok(
                GlobalResponse.success("Learning path retrieved successfully", response)
                        .withPath(httpRequest.getRequestURI()));
    }

    /**
     * Get all learning paths
     * GET /api/v1/learning-paths
     */
    @GetMapping
    public ResponseEntity<GlobalResponse<List<LearningPathResponse>>> getAllLearningPaths(
            @RequestParam(required = false, defaultValue = "false") boolean includeInactive,
            HttpServletRequest httpRequest) {

        log.info("Retrieving all learning paths. Include inactive: {}", includeInactive);

        List<LearningPathResponse> response = includeInactive
                ? learningPathService.getAllLearningPaths()
                : learningPathService.getActiveLearningPaths();

        return ResponseEntity.ok(
                GlobalResponse.success("Learning paths retrieved successfully", response)
                        .withPath(httpRequest.getRequestURI()));
    }

    /**
     * Search learning paths by keyword
     * GET /api/v1/learning-paths/search
     */
    @GetMapping("/search")
    public ResponseEntity<GlobalResponse<List<LearningPathResponse>>> searchLearningPaths(
            @RequestParam String keyword,
            HttpServletRequest httpRequest) {

        log.info("Searching learning paths with keyword: {}", keyword);

        List<LearningPathResponse> response = learningPathService.searchLearningPaths(keyword);

        return ResponseEntity.ok(
                GlobalResponse.success("Search completed successfully", response)
                        .withPath(httpRequest.getRequestURI()));
    }

    /**
     * Add a course to a learning path
     * POST /api/v1/learning-paths/{pathId}/courses
     */
    @PostMapping("/{pathId}/courses")
    public ResponseEntity<GlobalResponse<Void>> addCourseToPath(
            @PathVariable UUID pathId,
            @Valid @RequestBody CourseOrderRequest request,
            @RequestParam(required = false) UUID updatedBy,
            HttpServletRequest httpRequest) {

        log.info("Adding course {} to learning path {}", request.getCourseId(), pathId);

        UUID updater = updatedBy != null ? updatedBy : UUID.randomUUID(); // TODO: Get from JWT
        learningPathService.addCourseToPath(pathId, request, updater);

        return ResponseEntity.ok(
                GlobalResponse.<Void>success("Course added to learning path successfully", null)
                        .withStatus("COURSE_ADDED")
                        .withPath(httpRequest.getRequestURI()));
    }

    /**
     * Remove a course from a learning path
     * DELETE /api/v1/learning-paths/{pathId}/courses/{courseId}
     */
    @DeleteMapping("/{pathId}/courses/{courseId}")
    public ResponseEntity<GlobalResponse<Void>> removeCourseFromPath(
            @PathVariable UUID pathId,
            @PathVariable UUID courseId,
            @RequestParam(required = false) UUID updatedBy,
            HttpServletRequest httpRequest) {

        log.info("Removing course {} from learning path {}", courseId, pathId);

        UUID updater = updatedBy != null ? updatedBy : UUID.randomUUID(); // TODO: Get from JWT
        learningPathService.removeCourseFromPath(pathId, courseId, updater);

        return ResponseEntity.ok(
                GlobalResponse.<Void>success("Course removed from learning path successfully", null)
                        .withStatus("COURSE_REMOVED")
                        .withPath(httpRequest.getRequestURI()));
    }

    /**
     * Update course order in a learning path
     * PUT /api/v1/learning-paths/{pathId}/courses/order
     */
    @PutMapping("/{pathId}/courses/order")
    public ResponseEntity<GlobalResponse<Void>> updateCourseOrder(
            @PathVariable UUID pathId,
            @Valid @RequestBody List<CourseOrderRequest> courses,
            @RequestParam(required = false) UUID updatedBy,
            HttpServletRequest httpRequest) {

        log.info("Updating course order for learning path: {}", pathId);

        UUID updater = updatedBy != null ? updatedBy : UUID.randomUUID(); // TODO: Get from JWT
        learningPathService.updateCourseOrder(pathId, courses, updater);

        return ResponseEntity.ok(
                GlobalResponse.<Void>success("Course order updated successfully", null)
                        .withStatus("COURSE_ORDER_UPDATED")
                        .withPath(httpRequest.getRequestURI()));
    }

    /**
     * Get statistics for a learning path
     * GET /api/v1/learning-paths/{pathId}/stats
     */
    @GetMapping("/{pathId}/stats")
    public ResponseEntity<GlobalResponse<LearningPathStatsResponse>> getLearningPathStats(
            @PathVariable UUID pathId,
            HttpServletRequest httpRequest) {

        log.info("Retrieving statistics for learning path: {}", pathId);

        Integer totalEnrolled = learningPathService.getTotalEnrolled(pathId);
        Double averageCompletion = learningPathService.getAverageCompletion(pathId);

        LearningPathStatsResponse stats = LearningPathStatsResponse.builder()
                .pathId(pathId)
                .totalEnrolled(totalEnrolled)
                .averageCompletion(averageCompletion)
                .build();

        return ResponseEntity.ok(
                GlobalResponse.success("Statistics retrieved successfully", stats)
                        .withPath(httpRequest.getRequestURI()));
    }
}
