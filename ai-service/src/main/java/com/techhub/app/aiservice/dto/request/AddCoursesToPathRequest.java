package com.techhub.app.aiservice.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AddCoursesToPathRequest {

    private List<CourseInPath> courses;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CourseInPath {
        private UUID courseId;
        private Integer order;
        private Integer positionX;
        private Integer positionY;
        private String isOptional; // "Y" or "N"
    }
}
