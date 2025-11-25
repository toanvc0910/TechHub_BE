package com.techhub.app.courseservice.repository;

import com.techhub.app.courseservice.entity.Exercise;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ExerciseRepository extends JpaRepository<Exercise, UUID> {

    Optional<Exercise> findByLesson_IdAndIsActiveTrue(UUID lessonId);
    
    List<Exercise> findByLesson_IdAndIsActiveTrueOrderByOrderIndexAsc(UUID lessonId);
}
