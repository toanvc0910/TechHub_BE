package com.techhub.app.courseservice.repository;

import com.techhub.app.courseservice.entity.Submission;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SubmissionRepository extends JpaRepository<Submission, UUID> {

    List<Submission> findByExercise_IdAndUserIdAndIsActiveTrueOrderByCreatedDesc(UUID exerciseId, UUID userId);

    Optional<Submission> findTopByExercise_IdAndUserIdAndIsActiveTrueOrderByCreatedDesc(UUID exerciseId, UUID userId);
}
