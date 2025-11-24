package com.techhub.app.aiservice.dto.response;

import com.techhub.app.aiservice.enums.ChatMode;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;

import java.util.UUID;

@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ChatMessageResponse {
    private UUID sessionId;
    private UUID messageId;
    private ChatMode mode;
    private String message; // Unified field name
    private String answer; // Keep for backward compatibility
    private Object context;
}
