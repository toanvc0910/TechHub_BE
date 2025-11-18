package com.techhub.app.learningpathservice.service;

import java.util.List;
import java.util.UUID;

import com.techhub.app.learningpathservice.dto.request.CourseOrderRequest;
import com.techhub.app.learningpathservice.dto.request.CreateLearningPathRequest;
import com.techhub.app.learningpathservice.dto.request.UpdateLearningPathRequest;
import com.techhub.app.learningpathservice.dto.response.LearningPathResponse;

public interface LearningPathService {

    /**
     * Tạo learning path mới
     */
    LearningPathResponse createLearningPath(CreateLearningPathRequest request, UUID createdBy);

    /**
     * Cập nhật learning path
     */
    LearningPathResponse updateLearningPath(UUID id, UpdateLearningPathRequest request, UUID updatedBy);

    /**
     * Xóa learning path (soft delete)
     */
    void deleteLearningPath(UUID id, UUID deletedBy);

    /**
     * Lấy learning path theo ID
     */
    LearningPathResponse getLearningPathById(UUID id);

    /**
     * Lấy tất cả learning paths
     */
    List<LearningPathResponse> getAllLearningPaths();

    /**
     * Lấy learning paths đang active
     */
    List<LearningPathResponse> getActiveLearningPaths();

    /**
     * Tìm kiếm learning paths theo title
     */
    List<LearningPathResponse> searchLearningPaths(String keyword);

    /**
     * Thêm course vào learning path
     */
    void addCourseToPath(UUID pathId, CourseOrderRequest request, UUID updatedBy);

    /**
     * Xóa course khỏi learning path
     */
    void removeCourseFromPath(UUID pathId, UUID courseId, UUID updatedBy);

    /**
     * Cập nhật thứ tự courses trong path
     */
    void updateCourseOrder(UUID pathId, List<CourseOrderRequest> courses, UUID updatedBy);

    /**
     * Lấy số lượng users đang học path
     */
    Integer getTotalEnrolled(UUID pathId);

    /**
     * Lấy completion trung bình của path
     */
    Double getAverageCompletion(UUID pathId);
}