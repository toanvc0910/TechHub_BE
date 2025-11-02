package com.techhub.app.courseservice.repository;

import com.techhub.app.courseservice.entity.Lesson;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface LessonRepository extends JpaRepository<Lesson, UUID> {

    List<Lesson> findByChapter_IdAndIsActiveTrueOrderByOrderIndexAsc(UUID chapterId);

    Optional<Lesson> findByIdAndChapter_IdAndIsActiveTrue(UUID id, UUID chapterId);

    @Query("SELECT COALESCE(MAX(l.orderIndex), 0) FROM Lesson l " +
           "WHERE l.chapter.id = :chapterId AND l.isActive = true")
    Integer findMaxOrderIndexByChapterId(@Param("chapterId") UUID chapterId);
}
