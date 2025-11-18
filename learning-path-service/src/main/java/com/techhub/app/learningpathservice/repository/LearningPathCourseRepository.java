package com.techhub.app.learningpathservice.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.techhub.app.learningpathservice.entity.LearningPathCourse;

@Repository
public interface LearningPathCourseRepository extends JpaRepository<LearningPathCourse, UUID> {

    // Lấy tất cả courses trong path (sắp xếp theo order)
    List<LearningPathCourse> findByPathIdOrderByOrderAsc(UUID pathId);

    // Đếm số courses trong path
    long countByPathId(UUID pathId);

    // Xóa course khỏi path
    @Modifying
    @Query("DELETE FROM LearningPathCourse lpc WHERE lpc.pathId = :pathId AND lpc.courseId = :courseId")
    void removeFromPath(UUID pathId, UUID courseId);

    // Kiểm tra course có trong path không
    boolean existsByPathIdAndCourseId(UUID pathId, UUID courseId);
}
