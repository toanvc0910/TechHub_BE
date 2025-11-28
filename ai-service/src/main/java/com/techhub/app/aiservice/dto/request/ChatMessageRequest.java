package com.techhub.app.aiservice.dto.request;

import com.techhub.app.aiservice.enums.ChatMode;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.util.UUID;

@Data
public class ChatMessageRequest {

    private UUID sessionId;

    @NotNull
    private UUID userId;

    @NotNull
    private ChatMode mode = ChatMode.GENERAL;

    @NotBlank
    private String message;

    private Object context;
}
