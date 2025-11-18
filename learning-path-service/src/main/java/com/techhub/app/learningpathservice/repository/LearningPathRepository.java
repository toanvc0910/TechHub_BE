package com.techhub.app.learningpathservice.repository;

import com.techhub.app.learningpathservice.entity.LearningPath;
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
public interface LearningPathRepository extends JpaRepository<LearningPath, UUID> {

    Optional<LearningPath> findByIdAndIsActive(UUID id, Boolean isActive);

    Page<LearningPath> findByIsActive(Boolean isActive, Pageable pageable);

    @Query("SELECT lp FROM LearningPath lp WHERE lp.isActive = true AND " +
            "(LOWER(lp.title) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "LOWER(lp.description) LIKE LOWER(CONCAT('%', :keyword, '%')))")
    Page<LearningPath> searchLearningPaths(@Param("keyword") String keyword, Pageable pageable);

    @Query("SELECT lp FROM LearningPath lp WHERE lp.createdBy = :userId AND lp.isActive = true")
    Page<LearningPath> findByCreatedBy(@Param("userId") UUID userId, Pageable pageable);

    List<LearningPath> findByTitleContainingIgnoreCaseAndIsActive(String title, Boolean isActive);
}
