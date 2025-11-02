package com.techhub.app.courseservice.controller;

import com.techhub.app.commonservice.payload.GlobalResponse;
import com.techhub.app.courseservice.dto.request.LessonProgressRequest;
import com.techhub.app.courseservice.dto.response.CourseDetailResponse;
import com.techhub.app.courseservice.service.CourseProgressService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import java.util.UUID;

@RestController
@RequestMapping("/api/courses/{courseId}")
@RequiredArgsConstructor
@Slf4j
@Validated
public class CourseProgressController {

    private final CourseProgressService courseProgressService;

    @GetMapping("/progress")
    public ResponseEntity<GlobalResponse<CourseDetailResponse>> getCourseProgress(@PathVariable UUID courseId,
                                                                                  HttpServletRequest request) {
        CourseDetailResponse response = courseProgressService.getCourseProgress(courseId);
        return ResponseEntity.ok(
                GlobalResponse.success("Course progress retrieved", response)
                        .withPath(request.getRequestURI())
        );
    }

    @PutMapping("/lessons/{lessonId}/progress")
    public ResponseEntity<GlobalResponse<CourseDetailResponse>> updateLessonProgress(@PathVariable UUID courseId,
                                                                                      @PathVariable UUID lessonId,
                                                                                      @Valid @RequestBody LessonProgressRequest progressRequest,
                                                                                      HttpServletRequest request) {
        CourseDetailResponse response = courseProgressService.updateLessonProgress(courseId, lessonId, progressRequest);
        return ResponseEntity.ok(
                GlobalResponse.success("Lesson progress updated", response)
                        .withStatus("LESSON_PROGRESS_UPDATED")
                        .withPath(request.getRequestURI())
        );
    }

    @PostMapping("/lessons/{lessonId}/progress/complete")
    public ResponseEntity<GlobalResponse<CourseDetailResponse>> markLessonComplete(@PathVariable UUID courseId,
                                                                                   @PathVariable UUID lessonId,
                                                                                   HttpServletRequest request) {
        CourseDetailResponse response = courseProgressService.markLessonComplete(courseId, lessonId);
        return ResponseEntity.ok(
                GlobalResponse.success("Lesson marked as completed", response)
                        .withStatus("LESSON_COMPLETED")
                        .withPath(request.getRequestURI())
        );
    }
}
