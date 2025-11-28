package com.techhub.app.aiservice.repository;

import com.techhub.app.aiservice.entity.Recommendation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RecommendationRepository extends JpaRepository<Recommendation, UUID> {

    List<Recommendation> findByUserIdOrderByGeneratedAtDesc(UUID userId);

    Optional<Recommendation> findFirstByUserIdOrderByGeneratedAtDesc(UUID userId);
}
