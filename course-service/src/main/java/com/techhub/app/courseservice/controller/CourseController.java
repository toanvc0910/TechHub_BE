package com.techhub.app.courseservice.controller;

import com.techhub.app.commonservice.payload.GlobalResponse;
import com.techhub.app.commonservice.payload.PageGlobalResponse;
import com.techhub.app.courseservice.dto.request.ChapterRequest;
import com.techhub.app.courseservice.dto.request.CourseRequest;
import com.techhub.app.courseservice.dto.request.LessonAssetRequest;
import com.techhub.app.courseservice.dto.request.LessonRequest;
import com.techhub.app.courseservice.dto.response.ChapterResponse;
import com.techhub.app.courseservice.dto.response.CourseDetailResponse;
import com.techhub.app.courseservice.dto.response.CourseSummaryResponse;
import com.techhub.app.courseservice.dto.response.LessonAssetResponse;
import com.techhub.app.courseservice.dto.response.LessonResponse;
import com.techhub.app.courseservice.service.CourseService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/courses")
@RequiredArgsConstructor
@Slf4j
@Validated
public class CourseController {

    private final CourseService courseService;

    @GetMapping
    public ResponseEntity<PageGlobalResponse<CourseSummaryResponse>> getCourses(@RequestParam(defaultValue = "0") int page,
                                                                                @RequestParam(defaultValue = "10") int size,
                                                                                @RequestParam(required = false) String search,
                                                                                HttpServletRequest request) {
        Pageable pageable = PageRequest.of(page, size);
        Page<CourseSummaryResponse> coursePage = courseService.getCourses(search, pageable);

        PageGlobalResponse.PaginationInfo paginationInfo = PageGlobalResponse.PaginationInfo.builder()
                .page(coursePage.getNumber())
                .size(coursePage.getSize())
                .totalElements(coursePage.getTotalElements())
                .totalPages(coursePage.getTotalPages())
                .first(coursePage.isFirst())
                .last(coursePage.isLast())
                .hasNext(coursePage.hasNext())
                .hasPrevious(coursePage.hasPrevious())
                .build();

        return ResponseEntity.ok(
                PageGlobalResponse.success("Courses retrieved successfully", coursePage.getContent(), paginationInfo)
                        .withPath(request.getRequestURI())
        );
    }

    @GetMapping("/{courseId}")
    public ResponseEntity<GlobalResponse<CourseDetailResponse>> getCourse(@PathVariable UUID courseId,
                                                                          HttpServletRequest request) {
        CourseDetailResponse response = courseService.getCourse(courseId);
        return ResponseEntity.ok(
                GlobalResponse.success("Course retrieved successfully", response)
                        .withPath(request.getRequestURI())
        );
    }

    @PostMapping
    public ResponseEntity<GlobalResponse<CourseDetailResponse>> createCourse(@Valid @RequestBody CourseRequest requestBody,
                                                                             HttpServletRequest request) {
        CourseDetailResponse response = courseService.createCourse(requestBody);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(GlobalResponse.success("Course created successfully", response)
                        .withStatus("COURSE_CREATED")
                        .withPath(request.getRequestURI()));
    }

    @PutMapping("/{courseId}")
    public ResponseEntity<GlobalResponse<CourseDetailResponse>> updateCourse(@PathVariable UUID courseId,
                                                                             @Valid @RequestBody CourseRequest requestBody,
                                                                             HttpServletRequest request) {
        CourseDetailResponse response = courseService.updateCourse(courseId, requestBody);
        return ResponseEntity.ok(
                GlobalResponse.success("Course updated successfully", response)
                        .withStatus("COURSE_UPDATED")
                        .withPath(request.getRequestURI())
        );
    }

    @DeleteMapping("/{courseId}")
    public ResponseEntity<GlobalResponse<Void>> deleteCourse(@PathVariable UUID courseId,
                                                             HttpServletRequest request) {
        courseService.deleteCourse(courseId);
        return ResponseEntity.ok(
                GlobalResponse.<Void>success("Course deleted successfully", null)
                        .withStatus("COURSE_DELETED")
                        .withPath(request.getRequestURI())
        );
    }

    @PostMapping("/{courseId}/enroll")
    public ResponseEntity<GlobalResponse<Void>> enrollCourse(@PathVariable UUID courseId,
                                                             HttpServletRequest request) {
        courseService.enrollCourse(courseId);
        return ResponseEntity.ok(
                GlobalResponse.<Void>success("Enrolled in course successfully", null)
                        .withStatus("COURSE_ENROLLED")
                        .withPath(request.getRequestURI())
        );
    }

    @GetMapping("/{courseId}/chapters")
    public ResponseEntity<GlobalResponse<List<ChapterResponse>>> getChapters(@PathVariable UUID courseId,
                                                                             HttpServletRequest request) {
        List<ChapterResponse> chapters = courseService.getChapters(courseId);
        return ResponseEntity.ok(
                GlobalResponse.success("Chapters retrieved successfully", chapters)
                        .withPath(request.getRequestURI())
        );
    }

    @PostMapping("/{courseId}/chapters")
    public ResponseEntity<GlobalResponse<ChapterResponse>> createChapter(@PathVariable UUID courseId,
                                                                         @Valid @RequestBody ChapterRequest requestBody,
                                                                         HttpServletRequest request) {
        ChapterResponse response = courseService.createChapter(courseId, requestBody);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(GlobalResponse.success("Chapter created successfully", response)
                        .withStatus("CHAPTER_CREATED")
                        .withPath(request.getRequestURI()));
    }

    @PutMapping("/{courseId}/chapters/{chapterId}")
    public ResponseEntity<GlobalResponse<ChapterResponse>> updateChapter(@PathVariable UUID courseId,
                                                                         @PathVariable UUID chapterId,
                                                                         @Valid @RequestBody ChapterRequest requestBody,
                                                                         HttpServletRequest request) {
        ChapterResponse response = courseService.updateChapter(courseId, chapterId, requestBody);
        return ResponseEntity.ok(
                GlobalResponse.success("Chapter updated successfully", response)
                        .withStatus("CHAPTER_UPDATED")
                        .withPath(request.getRequestURI())
        );
    }

    @DeleteMapping("/{courseId}/chapters/{chapterId}")
    public ResponseEntity<GlobalResponse<Void>> deleteChapter(@PathVariable UUID courseId,
                                                              @PathVariable UUID chapterId,
                                                              HttpServletRequest request) {
        courseService.deleteChapter(courseId, chapterId);
        return ResponseEntity.ok(
                GlobalResponse.<Void>success("Chapter deleted successfully", null)
                        .withStatus("CHAPTER_DELETED")
                        .withPath(request.getRequestURI())
        );
    }

    @PostMapping("/{courseId}/chapters/{chapterId}/lessons")
    public ResponseEntity<GlobalResponse<LessonResponse>> createLesson(@PathVariable UUID courseId,
                                                                       @PathVariable UUID chapterId,
                                                                       @Valid @RequestBody LessonRequest requestBody,
                                                                       HttpServletRequest request) {
        LessonResponse response = courseService.createLesson(courseId, chapterId, requestBody);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(GlobalResponse.success("Lesson created successfully", response)
                        .withStatus("LESSON_CREATED")
                        .withPath(request.getRequestURI()));
    }

    @PutMapping("/{courseId}/chapters/{chapterId}/lessons/{lessonId}")
    public ResponseEntity<GlobalResponse<LessonResponse>> updateLesson(@PathVariable UUID courseId,
                                                                       @PathVariable UUID chapterId,
                                                                       @PathVariable UUID lessonId,
                                                                       @Valid @RequestBody LessonRequest requestBody,
                                                                       HttpServletRequest request) {
        LessonResponse response = courseService.updateLesson(courseId, chapterId, lessonId, requestBody);
        return ResponseEntity.ok(
                GlobalResponse.success("Lesson updated successfully", response)
                        .withStatus("LESSON_UPDATED")
                        .withPath(request.getRequestURI())
        );
    }

    @DeleteMapping("/{courseId}/chapters/{chapterId}/lessons/{lessonId}")
    public ResponseEntity<GlobalResponse<Void>> deleteLesson(@PathVariable UUID courseId,
                                                             @PathVariable UUID chapterId,
                                                             @PathVariable UUID lessonId,
                                                             HttpServletRequest request) {
        courseService.deleteLesson(courseId, chapterId, lessonId);
        return ResponseEntity.ok(
                GlobalResponse.<Void>success("Lesson deleted successfully", null)
                        .withStatus("LESSON_DELETED")
                        .withPath(request.getRequestURI())
        );
    }

    @PostMapping("/{courseId}/chapters/{chapterId}/lessons/{lessonId}/assets")
    public ResponseEntity<GlobalResponse<LessonAssetResponse>> createLessonAsset(@PathVariable UUID courseId,
                                                                                 @PathVariable UUID chapterId,
                                                                                 @PathVariable UUID lessonId,
                                                                                 @Valid @RequestBody LessonAssetRequest requestBody,
                                                                                 HttpServletRequest request) {
        LessonAssetResponse response = courseService.addLessonAsset(courseId, chapterId, lessonId, requestBody);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(GlobalResponse.success("Lesson asset created successfully", response)
                        .withStatus("LESSON_ASSET_CREATED")
                        .withPath(request.getRequestURI()));
    }

    @PutMapping("/{courseId}/chapters/{chapterId}/lessons/{lessonId}/assets/{assetId}")
    public ResponseEntity<GlobalResponse<LessonAssetResponse>> updateLessonAsset(@PathVariable UUID courseId,
                                                                                 @PathVariable UUID chapterId,
                                                                                 @PathVariable UUID lessonId,
                                                                                 @PathVariable UUID assetId,
                                                                                 @Valid @RequestBody LessonAssetRequest requestBody,
                                                                                 HttpServletRequest request) {
        LessonAssetResponse response = courseService.updateLessonAsset(courseId, chapterId, lessonId, assetId, requestBody);
        return ResponseEntity.ok(
                GlobalResponse.success("Lesson asset updated successfully", response)
                        .withStatus("LESSON_ASSET_UPDATED")
                        .withPath(request.getRequestURI())
        );
    }

    @DeleteMapping("/{courseId}/chapters/{chapterId}/lessons/{lessonId}/assets/{assetId}")
    public ResponseEntity<GlobalResponse<Void>> deleteLessonAsset(@PathVariable UUID courseId,
                                                                  @PathVariable UUID chapterId,
                                                                  @PathVariable UUID lessonId,
                                                                  @PathVariable UUID assetId,
                                                                  HttpServletRequest request) {
        courseService.deleteLessonAsset(courseId, chapterId, lessonId, assetId);
        return ResponseEntity.ok(
                GlobalResponse.<Void>success("Lesson asset deleted successfully", null)
                        .withStatus("LESSON_ASSET_DELETED")
                        .withPath(request.getRequestURI())
        );
    }
}
