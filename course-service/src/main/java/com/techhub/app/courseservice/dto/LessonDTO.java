package com.techhub.app.courseservice.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LessonDTO {
    private UUID id;
    private String title;
    private Integer order;
    private UUID chapterId;
    private String contentType;
    private String videoUrl;
    private List<String> documentUrls;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Boolean isActive;
}

