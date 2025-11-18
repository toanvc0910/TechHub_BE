package com.techhub.app.learningpathservice.dto.response;

import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonInclude;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL) // Chỉ include field không null
public class CourseInPathResponse {

    // === Từ LearningPathCourse entity ===
    private UUID courseId;
    private Integer order; // Thứ tự trong learning path

    // === Thông tin course (fetch từ Course Service qua API) ===
    private String title;
    private String description;
    private String thumbnail;
    private String instructor; // Tên giảng viên

    // Metadata
    private Integer duration; // Thời lượng (phút)
    private String level; // BEGINNER, INTERMEDIATE, ADVANCED
    private String category; // Backend, Frontend, DevOps, etc.

    // Statistics
    private Integer totalLessons;
    private Integer totalStudents;
    private Double rating; // 0-5
    private Integer totalReviews;

    // Progress (nếu có userId)
    private Boolean isEnrolled; // User đã enroll course này chưa
    private Float completion; // 0.0 - 1.0
    private Boolean isCompleted;

    // Pricing (optional)
    private String priceType; // FREE, PAID, PREMIUM
    private Double price;
    private String currency;
}