package com.techhub.app.courseservice.repository;

import com.techhub.app.courseservice.entity.Chapter;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ChapterRepository extends JpaRepository<Chapter, UUID> {

    List<Chapter> findByCourse_IdAndIsActiveTrueOrderByOrderIndexAsc(UUID courseId);

    Optional<Chapter> findByIdAndCourse_IdAndIsActiveTrue(UUID id, UUID courseId);

    @Query("SELECT COALESCE(MAX(c.orderIndex), 0) FROM Chapter c " +
           "WHERE c.course.id = :courseId AND c.isActive = true")
    Integer findMaxOrderIndexByCourseId(@Param("courseId") UUID courseId);
}
