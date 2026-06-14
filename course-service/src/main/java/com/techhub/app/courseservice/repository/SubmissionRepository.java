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

    List<Submission> findByExercise_IdAndIsActiveTrueOrderByCreatedDesc(UUID exerciseId);

    Optional<Submission> findTopByExercise_IdAndUserIdAndIsActiveTrueOrderByCreatedDesc(UUID exerciseId, UUID userId);

    /**
     * Aggregate a lesson leaderboard from submissions.
     * Score is the sum of each user's best normalized grade per exercise.
     */
    @Query(value = "WITH best_per_exercise AS ( " +
            "    SELECT s.user_id, " +
            "           s.exercise_id, " +
            "           MAX(COALESCE(s.grade, 0)) AS best_grade, " +
            "           COUNT(*) AS attempts, " +
            "           MIN(s.created) AS first_at " +
            "    FROM submissions s " +
            "    JOIN exercises e ON e.id = s.exercise_id " +
            "    WHERE e.lesson_id = :lessonId " +
            "      AND s.status IN ('PASSED', 'FAILED', 'PARTIAL') " +
            "      AND s.is_active = 'Y' " +
            "      AND e.is_active = 'Y' " +
            "    GROUP BY s.user_id, s.exercise_id " +
            ") " +
            "SELECT user_id AS userId, " +
            "       COALESCE(SUM(best_grade / 100.0), 0) AS score, " +
            "       COALESCE(SUM(attempts), 0) AS attempts, " +
            "       MIN(first_at) AS firstAt " +
            "FROM best_per_exercise " +
            "GROUP BY user_id " +
            "ORDER BY score DESC, firstAt ASC " +
            "LIMIT :limit", nativeQuery = true)
    List<Object[]> findLessonLeaderboard(@Param("lessonId") UUID lessonId, @Param("limit") int limit);
}
