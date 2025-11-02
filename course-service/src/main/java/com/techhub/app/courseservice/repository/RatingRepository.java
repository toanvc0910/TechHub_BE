package com.techhub.app.courseservice.repository;

import com.techhub.app.courseservice.entity.Rating;
import com.techhub.app.courseservice.enums.RatingTarget;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RatingRepository extends JpaRepository<Rating, UUID> {

    Optional<Rating> findByUserIdAndTargetIdAndTargetTypeAndIsActiveTrue(UUID userId, UUID targetId, RatingTarget targetType);

    List<Rating> findByTargetIdAndTargetTypeAndIsActiveTrue(UUID targetId, RatingTarget targetType);

    @Query("SELECT AVG(r.score) FROM Rating r " +
           "WHERE r.targetId = :targetId " +
           "AND r.targetType = :targetType " +
           "AND r.isActive = true")
    Double getAverageScore(@Param("targetId") UUID targetId, @Param("targetType") RatingTarget targetType);

    long countByTargetIdAndTargetTypeAndIsActiveTrue(UUID targetId, RatingTarget targetType);
}
