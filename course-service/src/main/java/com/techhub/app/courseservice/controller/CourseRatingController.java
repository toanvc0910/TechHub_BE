package com.techhub.app.courseservice.controller;

import com.techhub.app.commonservice.payload.GlobalResponse;
import com.techhub.app.courseservice.dto.request.RatingRequest;
import com.techhub.app.courseservice.dto.response.CourseRatingResponse;
import com.techhub.app.courseservice.service.CourseRatingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import java.util.UUID;

@RestController
@RequestMapping("/api/courses/{courseId}/ratings")
@RequiredArgsConstructor
@Slf4j
@Validated
public class CourseRatingController {

    private final CourseRatingService courseRatingService;

    @GetMapping
    public ResponseEntity<GlobalResponse<CourseRatingResponse>> getCourseRating(@PathVariable UUID courseId,
            HttpServletRequest request) {
        CourseRatingResponse response = courseRatingService.getCourseRating(courseId);
        return ResponseEntity.ok(
                GlobalResponse.success("Course rating retrieved", response)
                        .withPath(request.getRequestURI()));
    }

    @PostMapping
    public ResponseEntity<GlobalResponse<CourseRatingResponse>> submitCourseRating(@PathVariable UUID courseId,
            @Valid @RequestBody RatingRequest ratingRequest,
            HttpServletRequest request) {
        CourseRatingResponse response = courseRatingService.submitCourseRating(courseId, ratingRequest);
        return ResponseEntity.ok(
                GlobalResponse.success("Course rating submitted", response)
                        .withStatus("COURSE_RATED")
                        .withPath(request.getRequestURI()));
    }
}
