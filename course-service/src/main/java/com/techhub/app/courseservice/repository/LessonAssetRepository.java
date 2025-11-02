package com.techhub.app.courseservice.repository;

import com.techhub.app.courseservice.entity.LessonAsset;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface LessonAssetRepository extends JpaRepository<LessonAsset, UUID> {

    List<LessonAsset> findByLesson_IdAndIsActiveTrueOrderByOrderIndexAsc(UUID lessonId);

    Optional<LessonAsset> findByIdAndLesson_IdAndIsActiveTrue(UUID id, UUID lessonId);

    @Query("SELECT COALESCE(MAX(a.orderIndex), 0) FROM LessonAsset a " +
           "WHERE a.lesson.id = :lessonId AND a.isActive = true")
    Integer findMaxOrderIndexByLessonId(@Param("lessonId") UUID lessonId);
}
