package com.techhub.app.proxyclient.controller;

import com.techhub.app.proxyclient.client.LearningPathServiceClient;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/proxy/learning-paths")
@RequiredArgsConstructor
public class LearningPathProxyController {

    private final LearningPathServiceClient learningPathServiceClient;

    // Learning Path CRUD
    @GetMapping
    public ResponseEntity<String> getAllLearningPaths(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "created") String sortBy,
            @RequestParam(defaultValue = "DESC") String sortDirection) {
        return learningPathServiceClient.getAllLearningPaths(page, size, sortBy, sortDirection);
    }

    @PostMapping
    public ResponseEntity<String> createLearningPath(
            @RequestBody Object createRequest,
            @RequestHeader("Authorization") String authHeader) {
        return learningPathServiceClient.createLearningPath(createRequest, authHeader);
    }

    @GetMapping("/{id}")
    public ResponseEntity<String> getLearningPathById(
            @PathVariable String id,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        return learningPathServiceClient.getLearningPathById(id, authHeader);
    }

    @PutMapping("/{id}")
    public ResponseEntity<String> updateLearningPath(
            @PathVariable String id,
            @RequestBody Object updateRequest,
            @RequestHeader("Authorization") String authHeader) {
        return learningPathServiceClient.updateLearningPath(id, updateRequest, authHeader);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<String> deleteLearningPath(
            @PathVariable String id,
            @RequestHeader("Authorization") String authHeader) {
        return learningPathServiceClient.deleteLearningPath(id, authHeader);
    }

    // Search & Filter
    @GetMapping("/search")
    public ResponseEntity<String> searchLearningPaths(
            @RequestParam String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return learningPathServiceClient.searchLearningPaths(keyword, page, size);
    }

    @GetMapping("/creator/{userId}")
    public ResponseEntity<String> getLearningPathsByCreator(
            @PathVariable String userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return learningPathServiceClient.getLearningPathsByCreator(userId, page, size);
    }

    @GetMapping("/by-course/{courseId}")
    public ResponseEntity<String> getLearningPathsByCourse(
            @PathVariable String courseId) {
        return learningPathServiceClient.getLearningPathsByCourse(courseId);
    }

    // Course Management
    @PostMapping("/{pathId}/courses")
    public ResponseEntity<String> addCoursesToPath(
            @PathVariable String pathId,
            @RequestBody Object request,
            @RequestHeader("Authorization") String authHeader) {
        return learningPathServiceClient.addCoursesToPath(pathId, request, authHeader);
    }

    @DeleteMapping("/{pathId}/courses/{courseId}")
    public ResponseEntity<String> removeCourseFromPath(
            @PathVariable String pathId,
            @PathVariable String courseId,
            @RequestHeader("Authorization") String authHeader) {
        return learningPathServiceClient.removeCourseFromPath(pathId, courseId, authHeader);
    }

    @PutMapping("/{pathId}/courses/reorder")
    public ResponseEntity<String> reorderCourses(
            @PathVariable String pathId,
            @RequestBody Object request,
            @RequestHeader("Authorization") String authHeader) {
        return learningPathServiceClient.reorderCourses(pathId, request, authHeader);
    }

    // Progress Tracking
    @PostMapping("/progress")
    public ResponseEntity<String> createOrUpdateProgress(
            @RequestBody Object request,
            @RequestHeader("Authorization") String authHeader) {
        return learningPathServiceClient.createOrUpdateProgress(request, authHeader);
    }

    @GetMapping("/progress/user/{userId}/path/{pathId}")
    public ResponseEntity<String> getProgressByUserAndPath(
            @PathVariable String userId,
            @PathVariable String pathId,
            @RequestHeader("Authorization") String authHeader) {
        return learningPathServiceClient.getProgressByUserAndPath(userId, pathId, authHeader);
    }

    @GetMapping("/progress/user/{userId}")
    public ResponseEntity<String> getProgressByUser(
            @PathVariable String userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestHeader("Authorization") String authHeader) {
        return learningPathServiceClient.getProgressByUser(userId, page, size, authHeader);
    }

    @GetMapping("/progress/path/{pathId}")
    public ResponseEntity<String> getProgressByPath(
            @PathVariable String pathId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        return learningPathServiceClient.getProgressByPath(pathId, page, size, authHeader);
    }

    @DeleteMapping("/progress/user/{userId}/path/{pathId}")
    public ResponseEntity<String> deleteProgress(
            @PathVariable String userId,
            @PathVariable String pathId,
            @RequestHeader("Authorization") String authHeader) {
        return learningPathServiceClient.deleteProgress(userId, pathId, authHeader);
    }

    @GetMapping("/{pathId}/statistics")
    public ResponseEntity<String> getPathStatistics(
            @PathVariable String pathId,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        return learningPathServiceClient.getPathStatistics(pathId, authHeader);
    }
}
