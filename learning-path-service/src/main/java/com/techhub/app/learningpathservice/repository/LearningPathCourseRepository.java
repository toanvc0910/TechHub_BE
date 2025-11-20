package com.techhub.app.learningpathservice.repository;

import com.techhub.app.learningpathservice.entity.LearningPathCourse;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface LearningPathCourseRepository
        extends JpaRepository<LearningPathCourse, LearningPathCourse.LearningPathCourseId> {

    List<LearningPathCourse> findByPathIdOrderByOrderAsc(UUID pathId);

    @Query("SELECT lpc FROM LearningPathCourse lpc WHERE lpc.courseId = :courseId")
    List<LearningPathCourse> findByCourseId(@Param("courseId") UUID courseId);

    @Modifying
    @Query("DELETE FROM LearningPathCourse lpc WHERE lpc.pathId = :pathId")
    void deleteByPathId(@Param("pathId") UUID pathId);

    @Modifying
    @Query("DELETE FROM LearningPathCourse lpc WHERE lpc.pathId = :pathId AND lpc.courseId = :courseId")
    void deleteByPathIdAndCourseId(@Param("pathId") UUID pathId, @Param("courseId") UUID courseId);

    boolean existsByPathIdAndCourseId(UUID pathId, UUID courseId);
}
