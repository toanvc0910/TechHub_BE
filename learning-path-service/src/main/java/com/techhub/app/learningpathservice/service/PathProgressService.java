package com.techhub.app.learningpathservice.service;

import java.util.List;
import java.util.UUID;

import com.techhub.app.learningpathservice.dto.request.UpdatePathProgressRequest;
import com.techhub.app.learningpathservice.dto.response.PathProgressResponse;

public interface PathProgressService {

    /**
     * Bắt đầu học một learning path
     */
    PathProgressResponse startLearningPath(UUID userId, UUID pathId);

    /**
     * Cập nhật tiến độ học tập
     */
    PathProgressResponse updateProgress(UpdatePathProgressRequest request);

    /**
     * Lấy tiến độ của user trên một path
     */
    PathProgressResponse getProgressByUserAndPath(UUID userId, UUID pathId);

    /**
     * Lấy tất cả tiến độ của user
     */
    List<PathProgressResponse> getAllProgressByUser(UUID userId);

    /**
     * Lấy tất cả users đang học path
     */
    List<PathProgressResponse> getAllProgressByPath(UUID pathId);

    /**
     * Kiểm tra user đã hoàn thành path chưa
     */
    Boolean isPathCompleted(UUID userId, UUID pathId);

    /**
     * Lấy danh sách users đã hoàn thành path
     */
    List<PathProgressResponse> getCompletedUsers(UUID pathId);

    /**
     * Tính completion dựa trên các courses đã hoàn thành
     */
    Float calculateCompletion(UUID userId, UUID pathId);
}