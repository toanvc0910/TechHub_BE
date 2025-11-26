package com.techhub.app.courseservice.service.impl;

import com.techhub.app.commonservice.context.UserContext;
import com.techhub.app.commonservice.exception.ForbiddenException;
import com.techhub.app.commonservice.exception.NotFoundException;
import com.techhub.app.commonservice.exception.UnauthorizedException;
import com.techhub.app.courseservice.dto.request.RatingRequest;
import com.techhub.app.courseservice.dto.response.CourseRatingResponse;
import com.techhub.app.courseservice.entity.Course;
import com.techhub.app.courseservice.entity.Enrollment;
import com.techhub.app.courseservice.entity.Rating;
import com.techhub.app.courseservice.enums.CourseStatus;
import com.techhub.app.courseservice.enums.EnrollmentStatus;
import com.techhub.app.courseservice.enums.RatingTarget;
import com.techhub.app.courseservice.repository.CourseRepository;
import com.techhub.app.courseservice.repository.EnrollmentRepository;
import com.techhub.app.courseservice.repository.RatingRepository;
import com.techhub.app.courseservice.service.CourseRatingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class CourseRatingServiceImpl implements CourseRatingService {

    private final CourseRepository courseRepository;
    private final RatingRepository ratingRepository;
    private final EnrollmentRepository enrollmentRepository;

    @Override
    @Transactional(readOnly = true)
    public CourseRatingResponse getCourseRating(UUID courseId) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new NotFoundException("Course not found"));
        Double average = ratingRepository.getAverageScore(course.getId(), RatingTarget.COURSE.name());
        long count = ratingRepository.countByTargetIdAndTargetTypeAndIsActiveTrue(course.getId(), RatingTarget.COURSE);

        UUID currentUserId = UserContext.getCurrentUserId();
        Integer userScore = null;
        if (currentUserId != null) {
            userScore = ratingRepository
                    .findByUserIdAndTargetIdAndTargetTypeAndIsActiveTrue(currentUserId, courseId, RatingTarget.COURSE)
                    .map(Rating::getScore)
                    .orElse(null);
        }

        return CourseRatingResponse.builder()
                .courseId(courseId)
                .averageRating(average != null ? Math.round(average * 100d) / 100d : null)
                .ratingCount(count)
                .userScore(userScore)
                .build();
    }

    @Override
    public CourseRatingResponse submitCourseRating(UUID courseId, RatingRequest request) {
        UUID userId = requireCurrentUser();
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new NotFoundException("Course not found"));
        ensureCanRate(course, userId);

        Rating rating = ratingRepository
                .findByUserIdAndTargetIdAndTargetTypeAndIsActiveTrue(userId, courseId, RatingTarget.COURSE)
                .orElseGet(() -> {
                    Rating entity = new Rating();
                    entity.setUserId(userId);
                    entity.setTargetId(courseId);
                    entity.setTargetType(RatingTarget.COURSE);
                    entity.setCreatedBy(userId);
                    entity.setCreated(OffsetDateTime.now());
                    entity.setIsActive(true);
                    return entity;
                });

        rating.setScore(request.getScore());
        rating.setUpdatedBy(userId);
        rating.setUpdated(OffsetDateTime.now());
        ratingRepository.save(rating);
        log.debug("Course rating {} updated by {}", rating.getId(), userId);

        return getCourseRating(courseId);
    }

    private void ensureCanRate(Course course, UUID userId) {
        // Only ADMIN can bypass enrollment check
        if (UserContext.hasAnyRole("ADMIN")) {
            return;
        }

        // Check course status - only PUBLISHED courses can be rated
        if (course.getStatus() != CourseStatus.PUBLISHED) {
            throw new ForbiddenException("Only published courses can be rated");
        }

        // Check enrollment - must be enrolled and not dropped
        Enrollment enrollment = enrollmentRepository
                .findByUserIdAndCourse_IdAndIsActiveTrue(userId, course.getId())
                .orElseThrow(() -> new ForbiddenException("Only enrolled learners can rate this course"));

        // Reject DROPPED enrollments
        if (enrollment.getStatus() == EnrollmentStatus.DROPPED) {
            throw new ForbiddenException("You cannot rate a course you have dropped");
        }
    }

    private UUID requireCurrentUser() {
        UUID userId = UserContext.getCurrentUserId();
        if (userId == null) {
            throw new UnauthorizedException("Authentication required");
        }
        return userId;
    }
}
