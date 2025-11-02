package com.techhub.app.courseservice.repository;

import com.techhub.app.courseservice.entity.Course;
import com.techhub.app.courseservice.entity.Enrollment;
import com.techhub.app.courseservice.enums.EnrollmentStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface EnrollmentRepository extends JpaRepository<Enrollment, UUID> {

    Optional<Enrollment> findByUserIdAndCourse_Id(UUID userId, UUID courseId);

    Optional<Enrollment> findByUserIdAndCourse_IdAndIsActiveTrue(UUID userId, UUID courseId);

    long countByCourseAndIsActiveTrue(Course course);

    long countByCourseAndStatus(Course course, EnrollmentStatus status);
}
