package com.techhub.app.courseservice.repository;

import com.techhub.app.courseservice.entity.Course;
import com.techhub.app.courseservice.entity.Enrollment;
import com.techhub.app.courseservice.enums.EnrollmentStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface EnrollmentRepository extends JpaRepository<Enrollment, UUID> {

    Optional<Enrollment> findByUserIdAndCourse_Id(UUID userId, UUID courseId);

    Optional<Enrollment> findByUserIdAndCourse_IdAndIsActiveTrue(UUID userId, UUID courseId);

    Optional<Enrollment> findByUserIdAndCourse_IdAndIsActive(UUID userId, UUID courseId, Boolean isActive);

    // Get all enrollments for a specific user
    List<Enrollment> findAllByUserIdAndIsActiveTrue(UUID userId);

    // Get enrollments by user and status
    List<Enrollment> findAllByUserIdAndStatusAndIsActiveTrue(UUID userId, EnrollmentStatus status);

    long countByCourseAndIsActiveTrue(Course course);

    long countByCourseAndStatus(Course course, EnrollmentStatus status);
}
