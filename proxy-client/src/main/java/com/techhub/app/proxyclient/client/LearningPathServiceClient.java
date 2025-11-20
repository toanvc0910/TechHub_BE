package com.techhub.app.proxyclient.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "LEARNING-PATH-SERVICE")
public interface LearningPathServiceClient {

    // Learning Path CRUD
    @GetMapping("/api/v1/learning-paths")
    ResponseEntity<String> getAllLearningPaths(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "created") String sortBy,
            @RequestParam(defaultValue = "DESC") String sortDirection);

    @PostMapping("/api/v1/learning-paths")
    ResponseEntity<String> createLearningPath(
            @RequestBody Object createRequest,
            @RequestHeader("Authorization") String authHeader);

    @GetMapping("/api/v1/learning-paths/{id}")
    ResponseEntity<String> getLearningPathById(
            @PathVariable String id,
            @RequestHeader(value = "Authorization", required = false) String authHeader);

    @PutMapping("/api/v1/learning-paths/{id}")
    ResponseEntity<String> updateLearningPath(
            @PathVariable String id,
            @RequestBody Object updateRequest,
            @RequestHeader("Authorization") String authHeader);

    @DeleteMapping("/api/v1/learning-paths/{id}")
    ResponseEntity<String> deleteLearningPath(
            @PathVariable String id,
            @RequestHeader("Authorization") String authHeader);

    // Search & Filter
    @GetMapping("/api/v1/learning-paths/search")
    ResponseEntity<String> searchLearningPaths(
            @RequestParam String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size);

    @GetMapping("/api/v1/learning-paths/creator/{userId}")
    ResponseEntity<String> getLearningPathsByCreator(
            @PathVariable String userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size);

    @GetMapping("/api/v1/learning-paths/by-course/{courseId}")
    ResponseEntity<String> getLearningPathsByCourse(
            @PathVariable String courseId);

    // Course Management
    @PostMapping("/api/v1/learning-paths/{pathId}/courses")
    ResponseEntity<String> addCoursesToPath(
            @PathVariable String pathId,
            @RequestBody Object request,
            @RequestHeader("Authorization") String authHeader);

    @DeleteMapping("/api/v1/learning-paths/{pathId}/courses/{courseId}")
    ResponseEntity<String> removeCourseFromPath(
            @PathVariable String pathId,
            @PathVariable String courseId,
            @RequestHeader("Authorization") String authHeader);

    @PutMapping("/api/v1/learning-paths/{pathId}/courses/reorder")
    ResponseEntity<String> reorderCourses(
            @PathVariable String pathId,
            @RequestBody Object request,
            @RequestHeader("Authorization") String authHeader);

    // Progress Tracking
    @PostMapping("/api/v1/path-progress")
    ResponseEntity<String> createOrUpdateProgress(
            @RequestBody Object request,
            @RequestHeader("Authorization") String authHeader);

    @GetMapping("/api/v1/path-progress/user/{userId}/path/{pathId}")
    ResponseEntity<String> getProgressByUserAndPath(
            @PathVariable String userId,
            @PathVariable String pathId,
            @RequestHeader("Authorization") String authHeader);

    @GetMapping("/api/v1/path-progress/user/{userId}")
    ResponseEntity<String> getProgressByUser(
            @PathVariable String userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestHeader("Authorization") String authHeader);

    @GetMapping("/api/v1/path-progress/path/{pathId}")
    ResponseEntity<String> getProgressByPath(
            @PathVariable String pathId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestHeader(value = "Authorization", required = false) String authHeader);

    @DeleteMapping("/api/v1/path-progress/user/{userId}/path/{pathId}")
    ResponseEntity<String> deleteProgress(
            @PathVariable String userId,
            @PathVariable String pathId,
            @RequestHeader("Authorization") String authHeader);

    @GetMapping("/api/v1/path-progress/path/{pathId}/statistics")
    ResponseEntity<String> getPathStatistics(
            @PathVariable String pathId,
            @RequestHeader(value = "Authorization", required = false) String authHeader);
}
