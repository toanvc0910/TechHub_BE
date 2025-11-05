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

    Optional<Rating> findByUserIdAndTargetIdAndTargetTypeAndIsActiveTrue(UUID userId, UUID targetId,
            RatingTarget targetType);

    List<Rating> findByTargetIdAndTargetTypeAndIsActiveTrue(UUID targetId, RatingTarget targetType);

    @Query(value = "SELECT AVG(r.score) FROM ratings r " +
            "WHERE r.target_id = CAST(:targetId AS uuid) " +
            "AND r.target_type = CAST(:targetType AS rating_target) " +
            "AND r.is_active = 'Y'", nativeQuery = true)
    Double getAverageScore(@Param("targetId") UUID targetId, @Param("targetType") String targetType);

    long countByTargetIdAndTargetTypeAndIsActiveTrue(UUID targetId, RatingTarget targetType);
}
