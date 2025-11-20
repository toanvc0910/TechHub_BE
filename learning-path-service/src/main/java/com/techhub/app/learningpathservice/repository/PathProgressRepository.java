package com.techhub.app.learningpathservice.repository;

import com.techhub.app.learningpathservice.entity.PathProgress;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PathProgressRepository extends JpaRepository<PathProgress, UUID> {

    Optional<PathProgress> findByUserIdAndPathIdAndIsActive(UUID userId, UUID pathId, Boolean isActive);

    Page<PathProgress> findByUserIdAndIsActive(UUID userId, Boolean isActive, Pageable pageable);

    Page<PathProgress> findByPathIdAndIsActive(UUID pathId, Boolean isActive, Pageable pageable);

    @Query("SELECT pp FROM PathProgress pp WHERE pp.userId = :userId AND pp.completion = :completion AND pp.isActive = true")
    List<PathProgress> findByUserIdAndCompletion(@Param("userId") UUID userId, @Param("completion") Float completion);

    @Query("SELECT COUNT(pp) FROM PathProgress pp WHERE pp.pathId = :pathId AND pp.isActive = true")
    Long countByPathId(@Param("pathId") UUID pathId);

    @Query("SELECT AVG(pp.completion) FROM PathProgress pp WHERE pp.pathId = :pathId AND pp.isActive = true")
    Float getAverageCompletionByPathId(@Param("pathId") UUID pathId);
}
