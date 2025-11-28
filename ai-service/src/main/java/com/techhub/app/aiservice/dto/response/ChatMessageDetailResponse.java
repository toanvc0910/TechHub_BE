package com.techhub.app.aiservice.dto.response;

import com.techhub.app.aiservice.enums.ChatSender;
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
public class ChatMessageDetailResponse {
    private UUID id;
    private UUID sessionId;
    private ChatSender sender;
    private String content;
    private OffsetDateTime timestamp;
}
