package com.techhub.app.aiservice.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApproveExerciseDraftResponse {
    private UUID taskId;
    private UUID exerciseId; // Exercise ID từ Course Service
    private UUID lessonId;
    private String message;
    private Boolean success;
}
