package com.techhub.app.learningpathservice.dto.response;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonFormat;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LearningPathResponse {

    private UUID id;
    private String title;
    private String description;
    private List<String> skills;

    private Integer totalCourses;
    private Integer totalEnrolled; // Số users đang học
    private Double averageCompletion; // Completion trung bình

    private List<CourseInPathResponse> courses;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime created;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime updated;

    private UUID createdBy;
}