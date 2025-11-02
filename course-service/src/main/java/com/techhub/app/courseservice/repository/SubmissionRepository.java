package com.techhub.app.courseservice.repository;

import com.techhub.app.courseservice.model.Submission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface SubmissionRepository extends JpaRepository<Submission, UUID> {
    List<Submission> findByUserId(UUID userId);
    List<Submission> findByExerciseId(UUID exerciseId);
    List<Submission> findByUserIdAndExerciseId(UUID userId, UUID exerciseId);
    List<Submission> findByIsActiveTrue();
}

