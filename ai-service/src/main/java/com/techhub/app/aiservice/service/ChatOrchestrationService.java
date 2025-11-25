package com.techhub.app.aiservice.service;

import com.techhub.app.aiservice.dto.request.ChatMessageRequest;
import com.techhub.app.aiservice.dto.response.ChatMessageResponse;
import com.techhub.app.aiservice.dto.response.ChatSessionResponse;
import com.techhub.app.aiservice.enums.ChatMode;

import java.util.UUID;

public interface ChatOrchestrationService {

    ChatMessageResponse sendMessage(ChatMessageRequest request);

    ChatSessionResponse createSession(UUID userId, ChatMode mode);

    void deleteSession(UUID sessionId, UUID userId);

    void cleanupOldSessions(int daysToKeep);
}
