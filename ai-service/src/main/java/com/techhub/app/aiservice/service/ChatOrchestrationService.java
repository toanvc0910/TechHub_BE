package com.techhub.app.aiservice.service;

import com.techhub.app.aiservice.dto.request.ChatMessageRequest;
import com.techhub.app.aiservice.dto.response.ChatMessageResponse;

import java.util.UUID;

public interface ChatOrchestrationService {

    ChatMessageResponse sendMessage(ChatMessageRequest request);

    void deleteSession(UUID sessionId, UUID userId);

    void cleanupOldSessions(int daysToKeep);
}
