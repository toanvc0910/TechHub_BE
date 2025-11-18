
package com.techhub.app.learningpathservice.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.techhub.app.learningpathservice.entity.PathProgress;

@Repository
public interface PathProgressRepository extends JpaRepository<PathProgress, UUID> {

    // Tìm progress của user trên path cụ thể
    Optional<PathProgress> findByUserIdAndPathIdAndIsActive(UUID userId, UUID pathId, String isActive);

    // Tìm tất cả progress của user
    List<PathProgress> findByUserIdAndIsActive(UUID userId, String isActive);

    // Tìm tất cả progress trên path
    List<PathProgress> findByPathIdAndIsActive(UUID pathId, String isActive);

    // Tìm users đã hoàn thành path
    List<PathProgress> findByPathIdAndCompletionGreaterThanEqualAndIsActive(
            UUID pathId, Float completion, String isActive);

    // Đếm số users đang học path
    long countByPathIdAndIsActive(UUID pathId, String isActive);

    // Tìm tất cả progress active
    List<PathProgress> findByIsActive(String isActive);

    // Tính completion trung bình của path
    @Query("SELECT AVG(pp.completion) FROM PathProgress pp WHERE pp.pathId = :pathId AND pp.isActive = 'Y'")
    Double getAverageCompletion(UUID pathId);
}
