package com.techhub.app.courseservice.service;

import com.techhub.app.commonservice.context.UserContext;
import com.techhub.app.commonservice.enums.UserRole;
import com.techhub.app.commonservice.exception.ForbiddenException;
import com.techhub.app.commonservice.exception.NotFoundException;
import com.techhub.app.commonservice.exception.UnauthorizedException;
import com.techhub.app.courseservice.dto.request.CreateEnrollmentRequest;
import com.techhub.app.courseservice.dto.response.EnrollmentResponse;
import com.techhub.app.courseservice.entity.Course;
import com.techhub.app.courseservice.entity.Enrollment;
import com.techhub.app.courseservice.enums.EnrollmentStatus;
import com.techhub.app.courseservice.repository.CourseRepository;
import com.techhub.app.courseservice.repository.EnrollmentRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Slf4j
public class EnrollmentService {

    private final EnrollmentRepository enrollmentRepository;
    private final CourseRepository courseRepository;

    public EnrollmentService(EnrollmentRepository enrollmentRepository, CourseRepository courseRepository) {
        this.enrollmentRepository = enrollmentRepository;
        this.courseRepository = courseRepository;
    }

    @Transactional
    public EnrollmentResponse createEnrollment(CreateEnrollmentRequest request) {
        UUID targetUserId = resolveEnrollmentUserId(request.getUserId());
        log.info("Creating enrollment for user: {} and course: {}", targetUserId, request.getCourseId());

        Optional<Enrollment> existingEnrollment = enrollmentRepository
                .findByUserIdAndCourse_IdAndIsActive(targetUserId, request.getCourseId(), true);

        if (existingEnrollment.isPresent()) {
            log.info("User {} already enrolled in course {} - returning existing enrollment",
                    targetUserId, request.getCourseId());
            return mapToResponse(existingEnrollment.get());
        }

        Course course = courseRepository.findById(request.getCourseId())
                .orElseThrow(() -> new NotFoundException("Course not found: " + request.getCourseId()));

        Enrollment enrollment = new Enrollment();
        enrollment.setUserId(targetUserId);
        enrollment.setCourse(course);

        if (request.getStatus() != null && !request.getStatus().isEmpty()) {
            try {
                enrollment.setStatus(EnrollmentStatus.valueOf(request.getStatus().toUpperCase()));
            } catch (IllegalArgumentException e) {
                log.warn("Invalid enrollment status: {}, using default ENROLLED", request.getStatus());
                enrollment.setStatus(EnrollmentStatus.ENROLLED);
            }
        } else {
            enrollment.setStatus(EnrollmentStatus.ENROLLED);
        }

        enrollment.setCreatedBy(targetUserId);
        enrollment.setUpdatedBy(targetUserId);

        Enrollment savedEnrollment = enrollmentRepository.save(enrollment);
        log.info("Created enrollment {} for user {} and course {}",
                savedEnrollment.getId(), targetUserId, request.getCourseId());

        return mapToResponse(savedEnrollment);
    }

    public List<EnrollmentResponse> getUserEnrollments(UUID userId) {
        log.info("Getting all enrollments for user: {}", userId);
        List<Enrollment> enrollments = enrollmentRepository.findAllWithCourseByUserId(userId);
        log.info("Found {} enrollments for user: {}", enrollments.size(), userId);

        return enrollments.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public List<EnrollmentResponse> getUserEnrollmentsByStatus(UUID userId, EnrollmentStatus status) {
        log.info("Getting enrollments for user: {} with status: {}", userId, status);
        List<Enrollment> enrollments = enrollmentRepository.findAllWithCourseByUserIdAndStatus(userId, status);

        return enrollments.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public EnrollmentResponse getEnrollment(UUID enrollmentId) {
        log.info("Getting enrollment with ID: {}", enrollmentId);
        Enrollment enrollment = enrollmentRepository.findById(enrollmentId)
                .orElseThrow(() -> new NotFoundException("Enrollment not found: " + enrollmentId));
        ensureCanReadEnrollment(enrollment);
        return mapToResponse(enrollment);
    }

    private EnrollmentResponse mapToResponse(Enrollment enrollment) {
        return EnrollmentResponse.builder()
                .id(enrollment.getId())
                .userId(enrollment.getUserId())
                .courseId(enrollment.getCourse().getId())
                .courseName(enrollment.getCourse().getTitle())
                .thumbnail(enrollment.getCourse().getThumbnail())
                .status(enrollment.getStatus())
                .enrolledAt(enrollment.getEnrolledAt())
                .completedAt(enrollment.getCompletedAt())
                .isActive(enrollment.getIsActive())
                .build();
    }

    private UUID resolveEnrollmentUserId(UUID requestedUserId) {
        UUID currentUserId = UserContext.getCurrentUserId();
        if (currentUserId == null) {
            throw new UnauthorizedException("Authentication required");
        }
        if (requestedUserId == null || requestedUserId.equals(currentUserId)) {
            return currentUserId;
        }
        if (UserContext.hasAnyRole(UserRole.ADMIN.name(), UserRole.SUPER_ADMIN.name())) {
            return requestedUserId;
        }
        throw new ForbiddenException("You are not allowed to create enrollment for another user");
    }

    private void ensureCanReadEnrollment(Enrollment enrollment) {
        UUID currentUserId = UserContext.getCurrentUserId();
        if (currentUserId == null) {
            throw new UnauthorizedException("Authentication required");
        }
        if (currentUserId.equals(enrollment.getUserId())
                || UserContext.hasAnyRole(UserRole.ADMIN.name(), UserRole.SUPER_ADMIN.name())) {
            return;
        }
        throw new ForbiddenException("You are not allowed to view this enrollment");
    }
}
