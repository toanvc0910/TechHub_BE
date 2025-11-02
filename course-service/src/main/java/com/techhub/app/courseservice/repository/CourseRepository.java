package com.techhub.app.courseservice.repository;

import com.techhub.app.courseservice.entity.Course;
import com.techhub.app.courseservice.enums.CourseStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface CourseRepository extends JpaRepository<Course, UUID> {

    @Query("SELECT c FROM Course c " +
           "WHERE c.isActive = true " +
           "AND (:status IS NULL OR c.status = :status) " +
           "AND (" +
           "  :search IS NULL OR " +
           "  LOWER(c.title) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "  LOWER(COALESCE(c.description, '')) LIKE LOWER(CONCAT('%', :search, '%'))" +
           ")")
    Page<Course> searchCourses(@Param("status") CourseStatus status,
                               @Param("search") String search,
                               Pageable pageable);

    @Query("SELECT c FROM Course c " +
           "WHERE c.isActive = true " +
           "AND c.instructorId = :instructorId " +
           "AND (" +
           "  :search IS NULL OR " +
           "  LOWER(c.title) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "  LOWER(COALESCE(c.description, '')) LIKE LOWER(CONCAT('%', :search, '%'))" +
           ")")
    Page<Course> searchInstructorCourses(@Param("instructorId") UUID instructorId,
                                         @Param("search") String search,
                                         Pageable pageable);

    Optional<Course> findByIdAndIsActiveTrue(UUID id);
}
