package com.techhub.app.courseservice.repository;

import com.techhub.app.courseservice.model.Course;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface CourseRepository extends JpaRepository<Course, UUID> {
    List<Course> findByInstructorId(UUID instructorId);
    List<Course> findByStatus(String status);
    List<Course> findByIsActiveTrue();
}

