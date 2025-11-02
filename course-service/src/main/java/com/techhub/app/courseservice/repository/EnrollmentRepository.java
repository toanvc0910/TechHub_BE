package com.techhub.app.courseservice.repository;

import com.techhub.app.courseservice.model.Enrollment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface EnrollmentRepository extends JpaRepository<Enrollment, UUID> {
    List<Enrollment> findByUserId(UUID userId);
    List<Enrollment> findByCourseId(UUID courseId);
    Optional<Enrollment> findByUserIdAndCourseId(UUID userId, UUID courseId);
    List<Enrollment> findByStatus(String status);
    List<Enrollment> findByIsActiveTrue();
}

