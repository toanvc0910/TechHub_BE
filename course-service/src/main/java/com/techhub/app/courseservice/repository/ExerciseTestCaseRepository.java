package com.techhub.app.courseservice.repository;

import com.techhub.app.courseservice.entity.ExerciseTestCase;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ExerciseTestCaseRepository extends JpaRepository<ExerciseTestCase, UUID> {

    List<ExerciseTestCase> findByExercise_IdAndIsActiveTrueOrderByOrderIndexAsc(UUID exerciseId);
}
