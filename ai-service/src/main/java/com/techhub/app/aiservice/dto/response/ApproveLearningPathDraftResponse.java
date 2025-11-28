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
public class ApproveLearningPathDraftResponse {
    private UUID taskId;
    private UUID learningPathId; // Learning Path ID từ Learning Path Service (nullable - FE sẽ tạo)
    private String message;
    private Boolean success;
    private Object learningPathData; // Draft data để FE dùng tạo learning path
}
