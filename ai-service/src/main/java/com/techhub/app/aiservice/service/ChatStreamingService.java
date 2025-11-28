package com.techhub.app.aiservice.service;

import com.techhub.app.aiservice.config.ChatbotProperties;
import com.techhub.app.aiservice.dto.request.ChatMessageRequest;
import com.techhub.app.aiservice.entity.ChatMessage;
import com.techhub.app.aiservice.entity.ChatSession;
import com.techhub.app.aiservice.enums.ChatMode;
import com.techhub.app.aiservice.enums.ChatSender;
import com.techhub.app.aiservice.repository.ChatMessageRepository;
import com.techhub.app.aiservice.repository.ChatSessionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;

import java.time.OffsetDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class ChatStreamingService {

    private final ChatSessionRepository chatSessionRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final OpenAiGateway openAiGateway;
    private final ChatbotProperties chatbotProperties;

    /**
     * Send a streaming chat message and return Flux of response chunks
     */
    @Transactional
    public Flux<String> sendStreamingMessage(ChatMessageRequest request) {
        log.info("📨 [ChatStreamingService] ===== PROCESSING STREAMING MESSAGE =====");
        log.info("📨 [ChatStreamingService] User: {}, Session: {}", request.getUserId(), request.getSessionId());
        log.info("📨 [ChatStreamingService] Message: {}", request.getMessage());

        // Load or create session
        ChatSession session = loadOrCreateSession(request);
        log.info("📨 [ChatStreamingService] Session loaded/created: {}", session.getId());

        // Save user message
        ChatMessage userMessage = new ChatMessage();
        userMessage.setSession(session);
        userMessage.setSender(ChatSender.USER);
        userMessage.setContent(request.getMessage());
        chatMessageRepository.save(userMessage);
        log.info("📨 [ChatStreamingService] User message saved");

        // Build messages list for OpenAI
        List<Map<String, String>> messages = buildMessageHistory(session, request);
        log.info("📨 [ChatStreamingService] Message history built, {} messages", messages.size());

        // Accumulate response for saving
        StringBuilder fullResponse = new StringBuilder();

        log.info("📨 [ChatStreamingService] Calling OpenAiGateway.generateStreamingResponseWithHistory...");
        return openAiGateway.generateStreamingResponseWithHistory(messages)
                .doOnNext(chunk -> {
                    log.info("📦 [ChatStreamingService] Received chunk from OpenAI: {}", chunk);
                    fullResponse.append(chunk);
                })
                .doOnComplete(() -> {
                    // Save bot message when stream completes
                    saveBotMessage(session, fullResponse.toString());
                    log.info("✅ [ChatStreamingService] Streaming complete, saved response ({} chars)",
                            fullResponse.length());
                })
                .doOnError(error -> {
                    log.error("❌ [ChatStreamingService] Streaming failed: {}", error.getMessage(), error);
                    saveBotMessage(session, "Xin lỗi, đã có lỗi xảy ra khi xử lý yêu cầu của bạn.");
                });
    }

    /**
     * Simple streaming without session management (for quick queries)
     */
    public Flux<String> streamSimpleResponse(String message, UUID userId) {
        log.info("📨 Processing simple streaming for user: {}", userId);

        return openAiGateway.generateStreamingResponse(message, chatbotProperties.getSystemPrompt());
    }

    private ChatSession loadOrCreateSession(ChatMessageRequest request) {
        if (request.getSessionId() != null) {
            return chatSessionRepository.findById(request.getSessionId())
                    .orElseGet(() -> createNewSession(request.getUserId(), request.getMode()));
        }
        return createNewSession(request.getUserId(), request.getMode());
    }

    private ChatSession createNewSession(UUID userId, ChatMode mode) {
        ChatSession session = new ChatSession();
        session.setUserId(userId);
        session.setStartedAt(OffsetDateTime.now());
        session.setContext(Map.of("mode", mode.name()));
        return chatSessionRepository.save(session);
    }

    private List<Map<String, String>> buildMessageHistory(ChatSession session, ChatMessageRequest request) {
        List<Map<String, String>> messages = new ArrayList<>();

        // Add system prompt
        messages.add(Map.of("role", "system", "content", chatbotProperties.getSystemPrompt()));

        // Get recent conversation history (last 10 messages)
        List<ChatMessage> recentMessages = chatMessageRepository
                .findTop10BySessionOrderByTimestampDesc(session);
        Collections.reverse(recentMessages); // Order from oldest to newest

        for (ChatMessage msg : recentMessages) {
            String role = msg.getSender() == ChatSender.USER ? "user" : "assistant";
            messages.add(Map.of("role", role, "content", msg.getContent()));
        }

        return messages;
    }

    private void saveBotMessage(ChatSession session, String content) {
        try {
            ChatMessage botMessage = new ChatMessage();
            botMessage.setSession(session);
            botMessage.setSender(ChatSender.BOT);
            botMessage.setContent(content);
            chatMessageRepository.save(botMessage);
        } catch (Exception e) {
            log.error("Failed to save bot message: {}", e.getMessage());
        }
    }
}
