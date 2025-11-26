package com.techhub.app.courseservice.service;

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

    public EnrollmentService(EnrollmentRepository enrollmentRepository,
            CourseRepository courseRepository) {
        this.enrollmentRepository = enrollmentRepository;
        this.courseRepository = courseRepository;
    }

    @Transactional
    public EnrollmentResponse createEnrollment(CreateEnrollmentRequest request) {
        log.info("📚 Creating enrollment for user: {} and course: {}", request.getUserId(), request.getCourseId());

        // Kiểm tra xem user đã enroll course này chưa
        Optional<Enrollment> existingEnrollment = enrollmentRepository
                .findByUserIdAndCourse_IdAndIsActive(request.getUserId(), request.getCourseId(), true);

        if (existingEnrollment.isPresent()) {
            log.info("ℹ️ User {} already enrolled in course {} - returning existing enrollment",
                    request.getUserId(), request.getCourseId());
            // Trả về enrollment hiện có - không throw exception
            return mapToResponse(existingEnrollment.get());
        }

        // Lấy thông tin course
        Course course = courseRepository.findById(request.getCourseId())
                .orElseThrow(() -> new RuntimeException("Course not found: " + request.getCourseId()));

        // Tạo enrollment mới
        Enrollment enrollment = new Enrollment();
        enrollment.setUserId(request.getUserId());
        enrollment.setCourse(course);

        // Set status
        if (request.getStatus() != null && !request.getStatus().isEmpty()) {
            try {
                enrollment.setStatus(EnrollmentStatus.valueOf(request.getStatus().toUpperCase()));
            } catch (IllegalArgumentException e) {
                log.warn("⚠️ Invalid status: {}, using default ENROLLED", request.getStatus());
                enrollment.setStatus(EnrollmentStatus.ENROLLED);
            }
        } else {
            enrollment.setStatus(EnrollmentStatus.ENROLLED);
        }

        enrollment.setCreatedBy(request.getUserId());
        enrollment.setUpdatedBy(request.getUserId());

        Enrollment savedEnrollment = enrollmentRepository.save(enrollment);
        log.info("✅ Successfully created enrollment with ID: {} for user: {} and course: {}",
                savedEnrollment.getId(), request.getUserId(), request.getCourseId());

        return mapToResponse(savedEnrollment);
    }

    /**
     * Get all enrollments for a user
     */
    public List<EnrollmentResponse> getUserEnrollments(UUID userId) {
        log.info("📖 Getting all enrollments for user: {}", userId);
        List<Enrollment> enrollments = enrollmentRepository.findAllByUserIdAndIsActiveTrue(userId);
        log.info("Found {} enrollments for user: {}", enrollments.size(), userId);

        return enrollments.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    /**
     * Get enrollments by user and status
     */
    public List<EnrollmentResponse> getUserEnrollmentsByStatus(UUID userId, EnrollmentStatus status) {
        log.info("📖 Getting enrollments for user: {} with status: {}", userId, status);
        List<Enrollment> enrollments = enrollmentRepository.findAllByUserIdAndStatusAndIsActiveTrue(userId, status);

        return enrollments.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    private EnrollmentResponse mapToResponse(Enrollment enrollment) {
        return EnrollmentResponse.builder()
                .id(enrollment.getId())
                .userId(enrollment.getUserId())
                .courseId(enrollment.getCourse().getId())
                .courseName(enrollment.getCourse().getTitle())
                .status(enrollment.getStatus())
                .enrolledAt(enrollment.getEnrolledAt())
                .completedAt(enrollment.getCompletedAt())
                .isActive(enrollment.getIsActive())
                .build();
    }

    public EnrollmentResponse getEnrollment(UUID enrollmentId) {
        log.info("Getting enrollment with ID: {}", enrollmentId);
        Enrollment enrollment = enrollmentRepository.findById(enrollmentId)
                .orElseThrow(() -> new RuntimeException("Enrollment not found: " + enrollmentId));
        return mapToResponse(enrollment);
    }
}