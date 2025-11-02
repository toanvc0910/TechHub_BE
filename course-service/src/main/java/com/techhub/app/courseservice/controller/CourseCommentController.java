package com.techhub.app.courseservice.controller;

import com.techhub.app.commonservice.payload.GlobalResponse;
import com.techhub.app.courseservice.dto.request.CommentRequest;
import com.techhub.app.courseservice.dto.response.CommentResponse;
import com.techhub.app.courseservice.service.CourseCommentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/courses/{courseId}")
@RequiredArgsConstructor
@Slf4j
@Validated
public class CourseCommentController {

    private final CourseCommentService courseCommentService;

    @GetMapping("/comments")
    public ResponseEntity<GlobalResponse<List<CommentResponse>>> getCourseComments(@PathVariable UUID courseId,
                                                                                   HttpServletRequest request) {
        List<CommentResponse> comments = courseCommentService.getCourseComments(courseId);
        return ResponseEntity.ok(
                GlobalResponse.success("Course comments retrieved", comments)
                        .withPath(request.getRequestURI())
        );
    }

    @PostMapping("/comments")
    public ResponseEntity<GlobalResponse<CommentResponse>> addCourseComment(@PathVariable UUID courseId,
                                                                            @Valid @RequestBody CommentRequest commentRequest,
                                                                            HttpServletRequest request) {
        CommentResponse response = courseCommentService.addCourseComment(courseId, commentRequest);
        return ResponseEntity.ok(
                GlobalResponse.success("Comment added", response)
                        .withStatus("COURSE_COMMENT_CREATED")
                        .withPath(request.getRequestURI())
        );
    }

    @GetMapping("/lessons/{lessonId}/comments")
    public ResponseEntity<GlobalResponse<List<CommentResponse>>> getLessonComments(@PathVariable UUID courseId,
                                                                                   @PathVariable UUID lessonId,
                                                                                   HttpServletRequest request) {
        List<CommentResponse> comments = courseCommentService.getLessonComments(courseId, lessonId);
        return ResponseEntity.ok(
                GlobalResponse.success("Lesson comments retrieved", comments)
                        .withPath(request.getRequestURI())
        );
    }

    @PostMapping("/lessons/{lessonId}/comments")
    public ResponseEntity<GlobalResponse<CommentResponse>> addLessonComment(@PathVariable UUID courseId,
                                                                            @PathVariable UUID lessonId,
                                                                            @Valid @RequestBody CommentRequest commentRequest,
                                                                            HttpServletRequest request) {
        CommentResponse response = courseCommentService.addLessonComment(courseId, lessonId, commentRequest);
        return ResponseEntity.ok(
                GlobalResponse.success("Comment added", response)
                        .withStatus("LESSON_COMMENT_CREATED")
                        .withPath(request.getRequestURI())
        );
    }

    @GetMapping("/lessons/{lessonId}/workspace/comments")
    public ResponseEntity<GlobalResponse<List<CommentResponse>>> getCodeComments(@PathVariable UUID courseId,
                                                                                @PathVariable UUID lessonId,
                                                                                HttpServletRequest request) {
        List<CommentResponse> comments = courseCommentService.getCodeComments(courseId, lessonId);
        return ResponseEntity.ok(
                GlobalResponse.success("Code comments retrieved", comments)
                        .withPath(request.getRequestURI())
        );
    }

    @PostMapping("/lessons/{lessonId}/workspace/comments")
    public ResponseEntity<GlobalResponse<CommentResponse>> addCodeComment(@PathVariable UUID courseId,
                                                                          @PathVariable UUID lessonId,
                                                                          @Valid @RequestBody CommentRequest commentRequest,
                                                                          HttpServletRequest request) {
        CommentResponse response = courseCommentService.addCodeComment(courseId, lessonId, commentRequest);
        return ResponseEntity.ok(
                GlobalResponse.success("Workspace comment added", response)
                        .withStatus("CODE_COMMENT_CREATED")
                        .withPath(request.getRequestURI())
        );
    }

    @DeleteMapping("/comments/{commentId}")
    public ResponseEntity<GlobalResponse<Void>> deleteComment(@PathVariable UUID courseId,
                                                              @PathVariable UUID commentId,
                                                              HttpServletRequest request) {
        courseCommentService.deleteComment(courseId, commentId);
        return ResponseEntity.ok(
                GlobalResponse.<Void>success("Comment deleted", null)
                        .withStatus("COMMENT_DELETED")
                        .withPath(request.getRequestURI())
        );
    }
}
