package com.techhub.app.aiservice.dto.response;

import com.techhub.app.aiservice.enums.AiTaskStatus;
import com.techhub.app.aiservice.enums.AiTaskType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiDraftListResponse {
    private UUID taskId;
    private AiTaskType taskType;
    private AiTaskStatus status;
    private String targetReference; // lesson_id hoặc goal
    private Object resultPayload; // Draft content từ AI
    private Object requestPayload; // Request gốc (optional)
    private String prompt; // Prompt đã dùng (optional)
    private OffsetDateTime createdAt;
}
