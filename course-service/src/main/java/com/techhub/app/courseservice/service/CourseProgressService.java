package com.techhub.app.courseservice.service;

import com.techhub.app.courseservice.dto.request.LessonProgressRequest;
import com.techhub.app.courseservice.dto.response.CourseDetailResponse;

import java.util.UUID;

public interface CourseProgressService {

    CourseDetailResponse updateLessonProgress(UUID courseId, UUID lessonId, LessonProgressRequest request);

    CourseDetailResponse markLessonComplete(UUID courseId, UUID lessonId);

    CourseDetailResponse getCourseProgress(UUID courseId);
}
