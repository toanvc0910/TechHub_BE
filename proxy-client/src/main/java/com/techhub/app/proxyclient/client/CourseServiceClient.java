package com.techhub.app.proxyclient.client;

import java.util.UUID;

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

@FeignClient(name = "COURSE-SERVICE")
public interface CourseServiceClient {

        // Course core operations
        @GetMapping("/api/courses")
        ResponseEntity<String> getAllCourses(@RequestParam(defaultValue = "0") int page,
                        @RequestParam(defaultValue = "10") int size,
                        @RequestParam(required = false) String search);

        @PostMapping("/api/courses")
        ResponseEntity<String> createCourse(@RequestBody Object createRequest,
                        @RequestHeader("Authorization") String authHeader);

        @GetMapping("/api/courses/{courseId}")
        ResponseEntity<String> getCourseById(@PathVariable String courseId,
                        @RequestHeader(value = "Authorization", required = false) String authHeader);

        @PutMapping("/api/courses/{courseId}")
        ResponseEntity<String> updateCourse(@PathVariable String courseId,
                        @RequestBody Object updateRequest,
                        @RequestHeader("Authorization") String authHeader);

        @DeleteMapping("/api/courses/{courseId}")
        ResponseEntity<String> deleteCourse(@PathVariable String courseId,
                        @RequestHeader("Authorization") String authHeader);

        // Enrollment & structure management
        @PostMapping("/api/courses/{courseId}/enroll")
        ResponseEntity<String> enrollCourse(@PathVariable String courseId,
                        @RequestHeader("Authorization") String authHeader);

        @GetMapping("/api/courses/{courseId}/chapters")
        ResponseEntity<String> getCourseChapters(@PathVariable String courseId,
                        @RequestHeader(value = "Authorization", required = false) String authHeader);

        @PostMapping("/api/courses/{courseId}/chapters")
        ResponseEntity<String> createChapter(@PathVariable String courseId,
                        @RequestBody Object request,
                        @RequestHeader("Authorization") String authHeader);

        @PutMapping("/api/courses/{courseId}/chapters/{chapterId}")
        ResponseEntity<String> updateChapter(@PathVariable String courseId,
                        @PathVariable String chapterId,
                        @RequestBody Object request,
                        @RequestHeader("Authorization") String authHeader);

        @DeleteMapping("/api/courses/{courseId}/chapters/{chapterId}")
        ResponseEntity<String> deleteChapter(@PathVariable String courseId,
                        @PathVariable String chapterId,
                        @RequestHeader("Authorization") String authHeader);

        @GetMapping("/api/courses/{courseId}/chapters/{chapterId}/lessons/{lessonId}/detail")
        ResponseEntity<String> getLesson(@PathVariable String courseId,
                        @PathVariable String chapterId,
                        @PathVariable String lessonId,
                        @RequestHeader(value = "Authorization", required = false) String authHeader);

        @PostMapping("/api/courses/{courseId}/chapters/{chapterId}/lessons")
        ResponseEntity<String> createLesson(@PathVariable String courseId,
                        @PathVariable String chapterId,
                        @RequestBody Object request,
                        @RequestHeader("Authorization") String authHeader);

        @PutMapping("/api/courses/{courseId}/chapters/{chapterId}/lessons/{lessonId}")
        ResponseEntity<String> updateLesson(@PathVariable String courseId,
                        @PathVariable String chapterId,
                        @PathVariable String lessonId,
                        @RequestBody Object request,
                        @RequestHeader("Authorization") String authHeader);

        @DeleteMapping("/api/courses/{courseId}/chapters/{chapterId}/lessons/{lessonId}")
        ResponseEntity<String> deleteLesson(@PathVariable String courseId,
                        @PathVariable String chapterId,
                        @PathVariable String lessonId,
                        @RequestHeader("Authorization") String authHeader);

        @PostMapping("/api/courses/{courseId}/chapters/{chapterId}/lessons/{lessonId}/assets")
        ResponseEntity<String> createLessonAsset(@PathVariable String courseId,
                        @PathVariable String chapterId,
                        @PathVariable String lessonId,
                        @RequestBody Object request,
                        @RequestHeader("Authorization") String authHeader);

        @PutMapping("/api/courses/{courseId}/chapters/{chapterId}/lessons/{lessonId}/assets/{assetId}")
        ResponseEntity<String> updateLessonAsset(@PathVariable String courseId,
                        @PathVariable String chapterId,
                        @PathVariable String lessonId,
                        @PathVariable String assetId,
                        @RequestBody Object request,
                        @RequestHeader("Authorization") String authHeader);

        @DeleteMapping("/api/courses/{courseId}/chapters/{chapterId}/lessons/{lessonId}/assets/{assetId}")
        ResponseEntity<String> deleteLessonAsset(@PathVariable String courseId,
                        @PathVariable String chapterId,
                        @PathVariable String lessonId,
                        @PathVariable String assetId,
                        @RequestHeader("Authorization") String authHeader);

        // Progress tracking
        @GetMapping("/api/courses/{courseId}/progress")
        ResponseEntity<String> getCourseProgress(@PathVariable String courseId,
                        @RequestHeader(value = "Authorization", required = false) String authHeader);

        @PutMapping("/api/courses/{courseId}/lessons/{lessonId}/progress")
        ResponseEntity<String> updateLessonProgress(@PathVariable String courseId,
                        @PathVariable String lessonId,
                        @RequestBody Object request,
                        @RequestHeader("Authorization") String authHeader);

        @PostMapping("/api/courses/{courseId}/lessons/{lessonId}/progress/complete")
        ResponseEntity<String> markLessonComplete(@PathVariable String courseId,
                        @PathVariable String lessonId,
                        @RequestHeader("Authorization") String authHeader);

        // Ratings
        @GetMapping("/api/courses/{courseId}/ratings")
        ResponseEntity<String> getCourseRating(@PathVariable String courseId,
                        @RequestHeader(value = "Authorization", required = false) String authHeader);

        @PostMapping("/api/courses/{courseId}/ratings")
        ResponseEntity<String> submitCourseRating(@PathVariable String courseId,
                        @RequestBody Object request,
                        @RequestHeader("Authorization") String authHeader);

        // Comments
        @GetMapping("/api/courses/{courseId}/comments")
        ResponseEntity<String> getCourseComments(@PathVariable String courseId,
                        @RequestHeader(value = "Authorization", required = false) String authHeader);

        @PostMapping("/api/courses/{courseId}/comments")
        ResponseEntity<String> addCourseComment(@PathVariable String courseId,
                        @RequestBody Object request,
                        @RequestHeader("Authorization") String authHeader);

        @GetMapping("/api/courses/{courseId}/lessons/{lessonId}/comments")
        ResponseEntity<String> getLessonComments(@PathVariable String courseId,
                        @PathVariable String lessonId,
                        @RequestHeader(value = "Authorization", required = false) String authHeader);

        @PostMapping("/api/courses/{courseId}/lessons/{lessonId}/comments")
        ResponseEntity<String> addLessonComment(@PathVariable String courseId,
                        @PathVariable String lessonId,
                        @RequestBody Object request,
                        @RequestHeader("Authorization") String authHeader);

        @GetMapping("/api/courses/{courseId}/lessons/{lessonId}/workspace/comments")
        ResponseEntity<String> getWorkspaceComments(@PathVariable String courseId,
                        @PathVariable String lessonId,
                        @RequestHeader(value = "Authorization", required = false) String authHeader);

        @PostMapping("/api/courses/{courseId}/lessons/{lessonId}/workspace/comments")
        ResponseEntity<String> addWorkspaceComment(@PathVariable String courseId,
                        @PathVariable String lessonId,
                        @RequestBody Object request,
                        @RequestHeader("Authorization") String authHeader);

        @DeleteMapping("/api/courses/{courseId}/comments/{commentId}")
        ResponseEntity<String> deleteComment(@PathVariable String courseId,
                        @PathVariable String commentId,
                        @RequestHeader("Authorization") String authHeader);

        // Exercises
        @GetMapping("/api/courses/{courseId}/lessons/{lessonId}/exercise")
        ResponseEntity<String> getExercise(@PathVariable String courseId,
                        @PathVariable String lessonId,
                        @RequestHeader(value = "Authorization", required = false) String authHeader);

        @PutMapping("/api/courses/{courseId}/lessons/{lessonId}/exercise")
        ResponseEntity<String> upsertExercise(@PathVariable String courseId,
                        @PathVariable String lessonId,
                        @RequestBody Object request,
                        @RequestHeader("Authorization") String authHeader);

        @PostMapping("/api/courses/{courseId}/lessons/{lessonId}/exercise/submissions")
        ResponseEntity<String> submitExercise(@PathVariable String courseId,
                        @PathVariable String lessonId,
                        @RequestBody Object request,
                        @RequestHeader("Authorization") String authHeader);

        // Workspace IDE
        @GetMapping("/api/courses/{courseId}/lessons/{lessonId}/workspace")
        ResponseEntity<String> getWorkspace(@PathVariable String courseId,
                        @PathVariable String lessonId,
                        @RequestHeader("Authorization") String authHeader);

        @PutMapping("/api/courses/{courseId}/lessons/{lessonId}/workspace")
        ResponseEntity<String> saveWorkspace(@PathVariable String courseId,
                        @PathVariable String lessonId,
                        @RequestBody Object request,
                        @RequestHeader("Authorization") String authHeader);

        // skill - forward raw response as String (JSON wrapper from course-service)
        @PostMapping("/api/courses/skills")
        ResponseEntity<String> createSkill(@RequestBody Object skillDTO);

        @GetMapping("/api/courses/skills/{id}")
        ResponseEntity<String> getSkill(@PathVariable("id") UUID id);

        @GetMapping("/api/courses/skills")
        ResponseEntity<String> getAllSkills();

        @PutMapping("/api/courses/skills/{id}")
        ResponseEntity<String> updateSkill(@PathVariable("id") UUID id, @RequestBody Object skillDTO);

        @DeleteMapping("/api/courses/skills/{id}")
        ResponseEntity<String> deleteSkill(@PathVariable("id") UUID id);

        // tags - forward raw response as String (JSON wrapper from course-service)
        @PostMapping("/api/courses/tags")
        ResponseEntity<String> createTag(@RequestBody Object tagDTO);

        @GetMapping("/api/courses/tags/{id}")
        ResponseEntity<String> getTag(@PathVariable("id") UUID id);

        @GetMapping("/api/courses/tags")
        ResponseEntity<String> getAllTags();

        @PutMapping("/api/courses/tags/{id}")
        ResponseEntity<String> updateTag(@PathVariable("id") UUID id, @RequestBody Object tagDTO);

        @DeleteMapping("/api/courses/tags/{id}")
        ResponseEntity<String> deleteTag(@PathVariable("id") UUID id);

        // ============================================
        // ENROLLMENTS
        // ============================================

        // Get current user's enrollments (My Learning)
        @GetMapping("/api/v1/enrollments/my-enrollments")
        ResponseEntity<String> getMyEnrollments(
                        @RequestParam(required = false) String status,
                        @RequestHeader(value = "Authorization", required = false) String authHeader);
}
