package com.techhub.app.courseservice.repository;

import com.techhub.app.courseservice.model.Rating;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface RatingRepository extends JpaRepository<Rating, UUID> {
    List<Rating> findByUserId(UUID userId);
    List<Rating> findByTargetId(UUID targetId);
    List<Rating> findByTargetIdAndTargetType(UUID targetId, String targetType);
    Optional<Rating> findByUserIdAndTargetIdAndTargetType(UUID userId, UUID targetId, String targetType);
    List<Rating> findByIsActiveTrue();
}
