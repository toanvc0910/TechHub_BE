package com.techhub.app.courseservice.service;

import com.techhub.app.courseservice.dto.request.CommentRequest;
import com.techhub.app.courseservice.dto.response.CommentResponse;

import java.util.List;
import java.util.UUID;

public interface CourseCommentService {

    List<CommentResponse> getCourseComments(UUID courseId);

    List<CommentResponse> getLessonComments(UUID courseId, UUID lessonId);

    CommentResponse addCourseComment(UUID courseId, CommentRequest request);

    CommentResponse addLessonComment(UUID courseId, UUID lessonId, CommentRequest request);

    List<CommentResponse> getCodeComments(UUID courseId, UUID lessonId);

    CommentResponse addCodeComment(UUID courseId, UUID lessonId, CommentRequest request);

    void deleteComment(UUID courseId, UUID commentId);
}
