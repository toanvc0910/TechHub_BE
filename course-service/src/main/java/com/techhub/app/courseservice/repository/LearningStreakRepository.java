package com.techhub.app.courseservice.repository;

import com.techhub.app.courseservice.entity.LearningStreak;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface LearningStreakRepository extends JpaRepository<LearningStreak, UUID> {

    Optional<LearningStreak> findByUserIdAndIsActiveTrue(UUID userId);
}
