package com.techhub.app.aiservice.dto.response;

import com.techhub.app.aiservice.enums.ChatMode;
import lombok.Builder;
import lombok.Data;

import java.util.UUID;

@Data
@Builder
public class ChatMessageResponse {
    private UUID sessionId;
    private UUID messageId;
    private ChatMode mode;
    private String answer;
    private Object context;
}
