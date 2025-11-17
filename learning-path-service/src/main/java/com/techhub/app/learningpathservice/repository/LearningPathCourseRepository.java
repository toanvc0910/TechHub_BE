package com.techhub.app.learningpathservice.repository;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.techhub.app.learningpathservice.entity.LearningPathCourse;

@Repository
public interface LearningPathCourseRepository extends JpaRepository<LearningPathCourse, UUID> {

}
