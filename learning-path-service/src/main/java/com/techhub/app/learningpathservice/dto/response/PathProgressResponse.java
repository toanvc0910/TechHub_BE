package com.techhub.app.learningpathservice.dto.response;

import java.time.LocalDateTime;
import java.util.Map;
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
public class PathProgressResponse {

    private UUID id;
    private UUID userId;
    private UUID pathId;
    private String pathTitle; // Thêm thông tin path

    private Float completion; // 0.0 - 1.0
    private Map<String, Object> milestones;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime created;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime updated;
}