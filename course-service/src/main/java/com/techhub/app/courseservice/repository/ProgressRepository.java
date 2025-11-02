package com.techhub.app.courseservice.repository;

import com.techhub.app.courseservice.entity.Progress;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ProgressRepository extends JpaRepository<Progress, UUID> {

    @Query("SELECT p FROM Progress p " +
           "JOIN FETCH p.lesson l " +
           "JOIN FETCH l.chapter c " +
           "WHERE p.userId = :userId " +
           "AND c.course.id = :courseId " +
           "AND p.isActive = true")
    List<Progress> findByUserAndCourse(@Param("userId") UUID userId, @Param("courseId") UUID courseId);

    @Query("SELECT p FROM Progress p " +
           "JOIN FETCH p.lesson l " +
           "WHERE p.userId = :userId " +
           "AND l.id = :lessonId")
    Optional<Progress> findByUserIdAndLessonId(@Param("userId") UUID userId, @Param("lessonId") UUID lessonId);

    long countByLesson_Chapter_Course_IdAndIsActiveTrue(UUID courseId);
}
