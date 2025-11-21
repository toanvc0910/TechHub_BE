package com.techhub.app.aiservice.service;

import com.techhub.app.aiservice.dto.request.ChatMessageRequest;
import com.techhub.app.aiservice.dto.response.ChatMessageResponse;

public interface ChatOrchestrationService {

    ChatMessageResponse sendMessage(ChatMessageRequest request);
}
