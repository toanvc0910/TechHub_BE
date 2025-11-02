package com.techhub.app.proxyclient.controller;

import com.techhub.app.proxyclient.client.CourseServiceClient;
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
@RequestMapping("/api/proxy/courses")
@RequiredArgsConstructor
public class CourseProxyController {

    private final CourseServiceClient courseServiceClient;

    // Course core operations
    @GetMapping
    public ResponseEntity<String> getAllCourses(@RequestParam(defaultValue = "0") int page,
                                                @RequestParam(defaultValue = "10") int size,
                                                @RequestParam(required = false) String search) {
        return courseServiceClient.getAllCourses(page, size, search);
    }

    @PostMapping
    public ResponseEntity<String> createCourse(@RequestBody Object createRequest,
                                               @RequestHeader("Authorization") String authHeader) {
        return courseServiceClient.createCourse(createRequest, authHeader);
    }

    @GetMapping("/{courseId}")
    public ResponseEntity<String> getCourseById(@PathVariable String courseId,
                                                @RequestHeader(value = "Authorization", required = false) String authHeader) {
        return courseServiceClient.getCourseById(courseId, authHeader);
    }

    @PutMapping("/{courseId}")
    public ResponseEntity<String> updateCourse(@PathVariable String courseId,
                                               @RequestBody Object updateRequest,
                                               @RequestHeader("Authorization") String authHeader) {
        return courseServiceClient.updateCourse(courseId, updateRequest, authHeader);
    }

    @DeleteMapping("/{courseId}")
    public ResponseEntity<String> deleteCourse(@PathVariable String courseId,
                                               @RequestHeader("Authorization") String authHeader) {
        return courseServiceClient.deleteCourse(courseId, authHeader);
    }

    // Enrollment
    @PostMapping("/{courseId}/enroll")
    public ResponseEntity<String> enrollCourse(@PathVariable String courseId,
                                               @RequestHeader("Authorization") String authHeader) {
        return courseServiceClient.enrollCourse(courseId, authHeader);
    }

    // Chapter management
    @GetMapping("/{courseId}/chapters")
    public ResponseEntity<String> getCourseChapters(@PathVariable String courseId,
                                                    @RequestHeader(value = "Authorization", required = false) String authHeader) {
        return courseServiceClient.getCourseChapters(courseId, authHeader);
    }

    @PostMapping("/{courseId}/chapters")
    public ResponseEntity<String> createChapter(@PathVariable String courseId,
                                                @RequestBody Object request,
                                                @RequestHeader("Authorization") String authHeader) {
        return courseServiceClient.createChapter(courseId, request, authHeader);
    }

    @PutMapping("/{courseId}/chapters/{chapterId}")
    public ResponseEntity<String> updateChapter(@PathVariable String courseId,
                                                @PathVariable String chapterId,
                                                @RequestBody Object request,
                                                @RequestHeader("Authorization") String authHeader) {
        return courseServiceClient.updateChapter(courseId, chapterId, request, authHeader);
    }

    @DeleteMapping("/{courseId}/chapters/{chapterId}")
    public ResponseEntity<String> deleteChapter(@PathVariable String courseId,
                                                @PathVariable String chapterId,
                                                @RequestHeader("Authorization") String authHeader) {
        return courseServiceClient.deleteChapter(courseId, chapterId, authHeader);
    }

    // Lesson management
    @PostMapping("/{courseId}/chapters/{chapterId}/lessons")
    public ResponseEntity<String> createLesson(@PathVariable String courseId,
                                               @PathVariable String chapterId,
                                               @RequestBody Object request,
                                               @RequestHeader("Authorization") String authHeader) {
        return courseServiceClient.createLesson(courseId, chapterId, request, authHeader);
    }

    @PutMapping("/{courseId}/chapters/{chapterId}/lessons/{lessonId}")
    public ResponseEntity<String> updateLesson(@PathVariable String courseId,
                                               @PathVariable String chapterId,
                                               @PathVariable String lessonId,
                                               @RequestBody Object request,
                                               @RequestHeader("Authorization") String authHeader) {
        return courseServiceClient.updateLesson(courseId, chapterId, lessonId, request, authHeader);
    }

    @DeleteMapping("/{courseId}/chapters/{chapterId}/lessons/{lessonId}")
    public ResponseEntity<String> deleteLesson(@PathVariable String courseId,
                                               @PathVariable String chapterId,
                                               @PathVariable String lessonId,
                                               @RequestHeader("Authorization") String authHeader) {
        return courseServiceClient.deleteLesson(courseId, chapterId, lessonId, authHeader);
    }

    // Lesson assets
    @PostMapping("/{courseId}/chapters/{chapterId}/lessons/{lessonId}/assets")
    public ResponseEntity<String> createLessonAsset(@PathVariable String courseId,
                                                    @PathVariable String chapterId,
                                                    @PathVariable String lessonId,
                                                    @RequestBody Object request,
                                                    @RequestHeader("Authorization") String authHeader) {
        return courseServiceClient.createLessonAsset(courseId, chapterId, lessonId, request, authHeader);
    }

    @PutMapping("/{courseId}/chapters/{chapterId}/lessons/{lessonId}/assets/{assetId}")
    public ResponseEntity<String> updateLessonAsset(@PathVariable String courseId,
                                                    @PathVariable String chapterId,
                                                    @PathVariable String lessonId,
                                                    @PathVariable String assetId,
                                                    @RequestBody Object request,
                                                    @RequestHeader("Authorization") String authHeader) {
        return courseServiceClient.updateLessonAsset(courseId, chapterId, lessonId, assetId, request, authHeader);
    }

    @DeleteMapping("/{courseId}/chapters/{chapterId}/lessons/{lessonId}/assets/{assetId}")
    public ResponseEntity<String> deleteLessonAsset(@PathVariable String courseId,
                                                    @PathVariable String chapterId,
                                                    @PathVariable String lessonId,
                                                    @PathVariable String assetId,
                                                    @RequestHeader("Authorization") String authHeader) {
        return courseServiceClient.deleteLessonAsset(courseId, chapterId, lessonId, assetId, authHeader);
    }

    // Progress tracking
    @GetMapping("/{courseId}/progress")
    public ResponseEntity<String> getCourseProgress(@PathVariable String courseId,
                                                    @RequestHeader(value = "Authorization", required = false) String authHeader) {
        return courseServiceClient.getCourseProgress(courseId, authHeader);
    }

    @PutMapping("/{courseId}/lessons/{lessonId}/progress")
    public ResponseEntity<String> updateLessonProgress(@PathVariable String courseId,
                                                       @PathVariable String lessonId,
                                                       @RequestBody Object request,
                                                       @RequestHeader("Authorization") String authHeader) {
        return courseServiceClient.updateLessonProgress(courseId, lessonId, request, authHeader);
    }

    @PostMapping("/{courseId}/lessons/{lessonId}/progress/complete")
    public ResponseEntity<String> markLessonComplete(@PathVariable String courseId,
                                                     @PathVariable String lessonId,
                                                     @RequestHeader("Authorization") String authHeader) {
        return courseServiceClient.markLessonComplete(courseId, lessonId, authHeader);
    }

    // Ratings
    @GetMapping("/{courseId}/ratings")
    public ResponseEntity<String> getCourseRating(@PathVariable String courseId,
                                                  @RequestHeader(value = "Authorization", required = false) String authHeader) {
        return courseServiceClient.getCourseRating(courseId, authHeader);
    }

    @PostMapping("/{courseId}/ratings")
    public ResponseEntity<String> submitCourseRating(@PathVariable String courseId,
                                                     @RequestBody Object request,
                                                     @RequestHeader("Authorization") String authHeader) {
        return courseServiceClient.submitCourseRating(courseId, request, authHeader);
    }

    // Comments
    @GetMapping("/{courseId}/comments")
    public ResponseEntity<String> getCourseComments(@PathVariable String courseId,
                                                    @RequestHeader(value = "Authorization", required = false) String authHeader) {
        return courseServiceClient.getCourseComments(courseId, authHeader);
    }

    @PostMapping("/{courseId}/comments")
    public ResponseEntity<String> addCourseComment(@PathVariable String courseId,
                                                   @RequestBody Object request,
                                                   @RequestHeader("Authorization") String authHeader) {
        return courseServiceClient.addCourseComment(courseId, request, authHeader);
    }

    @GetMapping("/{courseId}/lessons/{lessonId}/comments")
    public ResponseEntity<String> getLessonComments(@PathVariable String courseId,
                                                    @PathVariable String lessonId,
                                                    @RequestHeader(value = "Authorization", required = false) String authHeader) {
        return courseServiceClient.getLessonComments(courseId, lessonId, authHeader);
    }

    @PostMapping("/{courseId}/lessons/{lessonId}/comments")
    public ResponseEntity<String> addLessonComment(@PathVariable String courseId,
                                                   @PathVariable String lessonId,
                                                   @RequestBody Object request,
                                                   @RequestHeader("Authorization") String authHeader) {
        return courseServiceClient.addLessonComment(courseId, lessonId, request, authHeader);
    }

    @GetMapping("/{courseId}/lessons/{lessonId}/workspace/comments")
    public ResponseEntity<String> getWorkspaceComments(@PathVariable String courseId,
                                                       @PathVariable String lessonId,
                                                       @RequestHeader(value = "Authorization", required = false) String authHeader) {
        return courseServiceClient.getWorkspaceComments(courseId, lessonId, authHeader);
    }

    @PostMapping("/{courseId}/lessons/{lessonId}/workspace/comments")
    public ResponseEntity<String> addWorkspaceComment(@PathVariable String courseId,
                                                      @PathVariable String lessonId,
                                                      @RequestBody Object request,
                                                      @RequestHeader("Authorization") String authHeader) {
        return courseServiceClient.addWorkspaceComment(courseId, lessonId, request, authHeader);
    }

    @DeleteMapping("/{courseId}/comments/{commentId}")
    public ResponseEntity<String> deleteComment(@PathVariable String courseId,
                                                @PathVariable String commentId,
                                                @RequestHeader("Authorization") String authHeader) {
        return courseServiceClient.deleteComment(courseId, commentId, authHeader);
    }

    // Exercises
    @GetMapping("/{courseId}/lessons/{lessonId}/exercise")
    public ResponseEntity<String> getExercise(@PathVariable String courseId,
                                              @PathVariable String lessonId,
                                              @RequestHeader(value = "Authorization", required = false) String authHeader) {
        return courseServiceClient.getExercise(courseId, lessonId, authHeader);
    }

    @PutMapping("/{courseId}/lessons/{lessonId}/exercise")
    public ResponseEntity<String> upsertExercise(@PathVariable String courseId,
                                                 @PathVariable String lessonId,
                                                 @RequestBody Object request,
                                                 @RequestHeader("Authorization") String authHeader) {
        return courseServiceClient.upsertExercise(courseId, lessonId, request, authHeader);
    }

    @PostMapping("/{courseId}/lessons/{lessonId}/exercise/submissions")
    public ResponseEntity<String> submitExercise(@PathVariable String courseId,
                                                 @PathVariable String lessonId,
                                                 @RequestBody Object request,
                                                 @RequestHeader("Authorization") String authHeader) {
        return courseServiceClient.submitExercise(courseId, lessonId, request, authHeader);
    }

    // Workspace
    @GetMapping("/{courseId}/lessons/{lessonId}/workspace")
    public ResponseEntity<String> getWorkspace(@PathVariable String courseId,
                                               @PathVariable String lessonId,
                                               @RequestHeader("Authorization") String authHeader) {
        return courseServiceClient.getWorkspace(courseId, lessonId, authHeader);
    }

    @PutMapping("/{courseId}/lessons/{lessonId}/workspace")
    public ResponseEntity<String> saveWorkspace(@PathVariable String courseId,
                                                @PathVariable String lessonId,
                                                @RequestBody Object request,
                                                @RequestHeader("Authorization") String authHeader) {
        return courseServiceClient.saveWorkspace(courseId, lessonId, request, authHeader);
    }
}

