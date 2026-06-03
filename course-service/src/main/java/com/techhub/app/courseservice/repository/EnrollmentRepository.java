package com.techhub.app.courseservice.repository;

import com.techhub.app.courseservice.entity.Course;
import com.techhub.app.courseservice.entity.Enrollment;
import com.techhub.app.courseservice.enums.EnrollmentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface EnrollmentRepository extends JpaRepository<Enrollment, UUID> {

    Optional<Enrollment> findByUserIdAndCourse_Id(UUID userId, UUID courseId);

    Optional<Enrollment> findByUserIdAndCourse_IdAndIsActiveTrue(UUID userId, UUID courseId);

    Optional<Enrollment> findByUserIdAndCourse_IdAndIsActive(UUID userId, UUID courseId, Boolean isActive);

    // Get all enrollments for a specific user
    List<Enrollment> findAllByUserIdAndIsActiveTrue(UUID userId);

    // Same as above but eagerly fetches the course in one query (avoids N+1 in mapToResponse).
    @Query("SELECT e FROM Enrollment e JOIN FETCH e.course " +
           "WHERE e.userId = :userId AND e.isActive = true")
    List<Enrollment> findAllWithCourseByUserId(@Param("userId") UUID userId);

    // Get enrollments by user and status
    List<Enrollment> findAllByUserIdAndStatusAndIsActiveTrue(UUID userId, EnrollmentStatus status);

    // Same as above but eagerly fetches the course in one query (avoids N+1 in mapToResponse).
    @Query("SELECT e FROM Enrollment e JOIN FETCH e.course " +
           "WHERE e.userId = :userId AND e.status = :status AND e.isActive = true")
    List<Enrollment> findAllWithCourseByUserIdAndStatus(@Param("userId") UUID userId,
            @Param("status") EnrollmentStatus status);

    long countByCourseAndIsActiveTrue(Course course);

    // Batch count active enrollments for many courses at once (avoids N+1).
    @Query("SELECT e.course.id, COUNT(e) FROM Enrollment e " +
           "WHERE e.course.id IN :courseIds AND e.isActive = true GROUP BY e.course.id")
    List<Object[]> countActiveByCourseIds(@Param("courseIds") List<UUID> courseIds);

    long countByCourseAndStatus(Course course, EnrollmentStatus status);

    // Get all enrolled user IDs for a specific course
    List<Enrollment> findAllByCourse_IdAndIsActiveTrue(UUID courseId);

    // Get all active enrollments (for broadcast notifications)
    List<Enrollment> findAllByIsActiveTrue();
}
