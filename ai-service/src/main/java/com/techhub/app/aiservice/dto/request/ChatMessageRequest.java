package com.techhub.app.aiservice.dto.request;

import com.techhub.app.aiservice.enums.ChatMode;
import com.techhub.app.aiservice.validation.SafePrompt;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.util.UUID;

@Data
public class ChatMessageRequest {

    private UUID sessionId;

    @NotNull(message = "User ID is required")
    private UUID userId;

    @NotNull(message = "Chat mode is required")
    private ChatMode mode = ChatMode.GENERAL;

    @NotBlank(message = "Message cannot be empty")
    @Size(min = 1, max = 2000, message = "Message must be between 1 and 2000 characters")
    @SafePrompt
    private String message;

    private Object context;
}
