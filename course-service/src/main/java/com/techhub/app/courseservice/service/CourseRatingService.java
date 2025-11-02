package com.techhub.app.courseservice.service;

import com.techhub.app.courseservice.dto.request.RatingRequest;
import com.techhub.app.courseservice.dto.response.CourseRatingResponse;

import java.util.UUID;

public interface CourseRatingService {

    CourseRatingResponse getCourseRating(UUID courseId);

    CourseRatingResponse submitCourseRating(UUID courseId, RatingRequest request);
}
