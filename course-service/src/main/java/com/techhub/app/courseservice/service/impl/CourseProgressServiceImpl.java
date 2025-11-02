package com.techhub.app.courseservice.service.impl;

import com.techhub.app.commonservice.context.UserContext;
import com.techhub.app.commonservice.exception.ForbiddenException;
import com.techhub.app.commonservice.exception.NotFoundException;
import com.techhub.app.commonservice.exception.UnauthorizedException;
import com.techhub.app.courseservice.dto.request.LessonProgressRequest;
import com.techhub.app.courseservice.dto.response.CourseDetailResponse;
import com.techhub.app.courseservice.entity.Chapter;
import com.techhub.app.courseservice.entity.Course;
import com.techhub.app.courseservice.entity.Enrollment;
import com.techhub.app.courseservice.entity.Lesson;
import com.techhub.app.courseservice.entity.Progress;
import com.techhub.app.courseservice.repository.CourseRepository;
import com.techhub.app.courseservice.repository.EnrollmentRepository;
import com.techhub.app.courseservice.repository.LessonRepository;
import com.techhub.app.courseservice.repository.ProgressRepository;
import com.techhub.app.courseservice.service.CourseProgressService;
import com.techhub.app.courseservice.service.CourseService;
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
public class CourseProgressServiceImpl implements CourseProgressService {

    private final CourseRepository courseRepository;
    private final LessonRepository lessonRepository;
    private final ProgressRepository progressRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final CourseService courseService;

    @Override
    public CourseDetailResponse updateLessonProgress(UUID courseId, UUID lessonId, LessonProgressRequest request) {
        UUID userId = requireCurrentUser();
        Lesson lesson = resolveLesson(courseId, lessonId);
        ensureEnrollment(userId, courseId, lesson.getChapter().getCourse());

        Float completionValue = request.getCompletion();
        boolean markComplete = Boolean.TRUE.equals(request.getMarkComplete());

        Progress progress = progressRepository.findByUserIdAndLessonId(userId, lessonId)
                .orElseGet(() -> {
                    Progress entity = new Progress();
                    entity.setUserId(userId);
                    entity.setLesson(lesson);
                    entity.setCreatedBy(userId);
                    return entity;
                });

        if (completionValue != null) {
            completionValue = Math.max(0f, Math.min(1f, completionValue));
            progress.setCompletion(completionValue);
            if (completionValue >= 1f) {
                markComplete = true;
            }
        }

        if (markComplete) {
            progress.setCompletion(1f);
            progress.setCompletedAt(progress.getCompletedAt() == null ? OffsetDateTime.now() : progress.getCompletedAt());
        }

        progress.setUpdatedBy(userId);
        progressRepository.save(progress);
        log.debug("Progress updated for lesson {} by {} - completion {}", lessonId, userId, progress.getCompletion());

        return courseService.getCourse(courseId);
    }

    @Override
    public CourseDetailResponse markLessonComplete(UUID courseId, UUID lessonId) {
        LessonProgressRequest request = new LessonProgressRequest();
        request.setCompletion(1f);
        request.setMarkComplete(true);
        return updateLessonProgress(courseId, lessonId, request);
    }

    @Override
    @Transactional(readOnly = true)
    public CourseDetailResponse getCourseProgress(UUID courseId) {
        UUID userId = requireCurrentUser();
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new NotFoundException("Course not found"));
        ensureEnrollment(userId, courseId, course);
        return courseService.getCourse(courseId);
    }

    private UUID requireCurrentUser() {
        UUID userId = UserContext.getCurrentUserId();
        if (userId == null) {
            throw new UnauthorizedException("Authentication required");
        }
        return userId;
    }

    private Lesson resolveLesson(UUID courseId, UUID lessonId) {
        Lesson lesson = lessonRepository.findById(lessonId)
                .orElseThrow(() -> new NotFoundException("Lesson not found"));

        Chapter chapter = lesson.getChapter();
        if (chapter == null || chapter.getCourse() == null) {
            throw new NotFoundException("Lesson is not associated with a course");
        }
        UUID lessonCourseId = chapter.getCourse().getId();
        if (!lessonCourseId.equals(courseId)) {
            throw new ForbiddenException("Lesson does not belong to the specified course");
        }
        if (lesson.getIsActive() != null && !lesson.getIsActive()) {
            throw new NotFoundException("Lesson is not active");
        }
        return lesson;
    }

    private void ensureEnrollment(UUID userId, UUID courseId, Course course) {
        if (course == null) {
            course = courseRepository.findById(courseId)
                    .orElseThrow(() -> new NotFoundException("Course not found"));
        }
        boolean isAdmin = UserContext.hasAnyRole("ADMIN");
        boolean isInstructor = UserContext.hasAnyRole("INSTRUCTOR") && userId.equals(course.getInstructorId());
        if (isAdmin || isInstructor) {
            return;
        }
        Enrollment enrollment = enrollmentRepository.findByUserIdAndCourse_IdAndIsActiveTrue(userId, courseId)
                .orElse(null);
        if (enrollment == null) {
            throw new ForbiddenException("User is not enrolled in this course");
        }
    }
}
