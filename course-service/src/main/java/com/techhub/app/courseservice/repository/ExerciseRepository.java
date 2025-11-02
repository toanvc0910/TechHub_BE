package com.techhub.app.courseservice.repository;

import com.techhub.app.courseservice.model.Exercise;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ExerciseRepository extends JpaRepository<Exercise, UUID> {
    List<Exercise> findByLessonId(UUID lessonId);
    List<Exercise> findByType(String type);
    List<Exercise> findByIsActiveTrue();
}

