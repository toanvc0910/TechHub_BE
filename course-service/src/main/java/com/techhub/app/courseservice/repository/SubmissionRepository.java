package com.techhub.app.courseservice.repository;

import com.techhub.app.courseservice.entity.Submission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SubmissionRepository extends JpaRepository<Submission, UUID> {

    List<Submission> findByExercise_IdAndUserIdAndIsActiveTrueOrderByCreatedDesc(UUID exerciseId, UUID userId);

    Optional<Submission> findTopByExercise_IdAndUserIdAndIsActiveTrueOrderByCreatedDesc(UUID exerciseId, UUID userId);

    /**
     * Aggregate leaderboard for a lesson: tổng grade per user, tie-break ai xong
     * sớm nhất (MIN(created) ASC). Trả về [userId(UUID), score(Double), attempts(Long), firstAt(OffsetDateTime)].
     */
    @Query(value = "SELECT s.user_id AS userId, " +
            "       COALESCE(SUM(s.grade), 0) AS score, " +
            "       COUNT(*) AS attempts, " +
            "       MIN(s.created) AS firstAt " +
            "FROM submissions s " +
            "JOIN exercises e ON e.id = s.exercise_id " +
            "WHERE e.lesson_id = :lessonId " +
            "  AND s.status = 'GRADED' " +
            "  AND s.is_active = 'Y' " +
            "  AND e.is_active = 'Y' " +
            "GROUP BY s.user_id " +
            "ORDER BY score DESC, firstAt ASC " +
            "LIMIT :limit", nativeQuery = true)
    List<Object[]> findLessonLeaderboard(@Param("lessonId") UUID lessonId, @Param("limit") int limit);
}
