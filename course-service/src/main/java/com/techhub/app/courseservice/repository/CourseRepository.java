package com.techhub.app.courseservice.repository;

import com.techhub.app.courseservice.entity.Course;
import com.techhub.app.courseservice.enums.CourseStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface CourseRepository extends JpaRepository<Course, UUID> {
    Page<Course> findByTitleContainingIgnoreCase(String title, Pageable pageable);
    Page<Course> findByInstructorId(UUID instructorId, Pageable pageable);
    Page<Course> findByStatus(CourseStatus status, Pageable pageable);
}
