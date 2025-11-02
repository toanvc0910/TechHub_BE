package com.techhub.app.courseservice.repository;

import com.techhub.app.courseservice.model.Progress;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ProgressRepository extends JpaRepository<Progress, UUID> {
    List<Progress> findByUserId(UUID userId);
    List<Progress> findByLessonId(UUID lessonId);
    Optional<Progress> findByUserIdAndLessonId(UUID userId, UUID lessonId);
    List<Progress> findByIsActiveTrue();
}

