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
    private final VectorService vectorService;

    /**
     * Send a streaming chat message and return Flux of response chunks
     */
    @Transactional
    public Flux<String> sendStreamingMessage(ChatMessageRequest request) {
        log.info("📨 [ChatStreamingService] ===== PROCESSING STREAMING MESSAGE =====");
        log.info("📨 [ChatStreamingService] User: {}, Session: {}, Mode: {}",
                request.getUserId(), request.getSessionId(), request.getMode());
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

        // Build messages list for OpenAI with embedding context
        List<Map<String, String>> messages = buildMessageHistoryWithContext(session, request);
        log.info("📨 [ChatStreamingService] Message history built with context, {} messages", messages.size());

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
        session.setContext(Map.of("mode", mode != null ? mode.name() : ChatMode.GENERAL.name()));
        return chatSessionRepository.save(session);
    }

    /**
     * Build message history with embedding context for ADVISOR mode
     */
    @SuppressWarnings("unchecked")
    private List<Map<String, String>> buildMessageHistoryWithContext(ChatSession session, ChatMessageRequest request) {
        List<Map<String, String>> messages = new ArrayList<>();

        // Build system prompt with context based on mode
        String systemPrompt = buildSystemPromptWithContext(request);
        messages.add(Map.of("role", "system", "content", systemPrompt));

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

    /**
     * Build system prompt with embedding context for course recommendations
     */
    @SuppressWarnings("unchecked")
    private String buildSystemPromptWithContext(ChatMessageRequest request) {
        StringBuilder prompt = new StringBuilder();

        if (request.getMode() == ChatMode.ADVISOR) {
            // ADVISOR Mode: Search Qdrant for relevant courses
            prompt.append("Bạn là cố vấn học tập thông minh của TechHub - nền tảng học lập trình online.\n");
            prompt.append(
                    "Nhiệm vụ: Dựa trên câu hỏi của người dùng và danh sách khóa học bên dưới, hãy gợi ý khóa học phù hợp.\n");
            prompt.append("Hãy trả lời thân thiện, chi tiết và đề xuất cụ thể các khóa học nếu có.\n\n");
            prompt.append(
                    "⚠️ QUAN TRỌNG: Khi gợi ý khóa học, BẮT BUỘC phải bao gồm link dạng markdown: [Xem khóa học](/courses/{course_id})\n\n");

            log.info("🔍 [ADVISOR MODE - Streaming] Searching Qdrant for: {}", request.getMessage());

            List<Map<String, Object>> relevantCourses = null;
            try {
                relevantCourses = vectorService.searchCourses(request.getMessage(), 5);
                log.info("🔍 [ADVISOR MODE - Streaming] Found {} relevant courses",
                        relevantCourses != null ? relevantCourses.size() : 0);
            } catch (Exception e) {
                log.error("Failed to search courses from Qdrant: {}", e.getMessage(), e);
                relevantCourses = List.of();
            }

            if (relevantCourses != null && !relevantCourses.isEmpty()) {
                prompt.append("=== CÁC KHÓA HỌC LIÊN QUAN TỪ DATABASE ===\n\n");
                int count = 0;
                for (Map<String, Object> course : relevantCourses) {
                    Map<String, Object> payload = (Map<String, Object>) course.get("payload");
                    if (payload != null) {
                        Object courseId = payload.get("course_id");
                        prompt.append(String.format("**%d. %s**\n", ++count, payload.get("title")));
                        prompt.append("   - Mô tả: ").append(payload.get("description")).append("\n");
                        prompt.append("   - Trình độ: ").append(payload.get("level")).append("\n");
                        prompt.append("   - Course ID: ").append(courseId).append("\n");
                        prompt.append("   - 🔗 Link: [Xem khóa học](/courses/").append(courseId).append(")\n\n");
                    }
                }
                prompt.append("==========================================\n\n");
                prompt.append("Hãy gợi ý các khóa học phù hợp từ danh sách trên dựa trên câu hỏi của người dùng.\n");
                prompt.append("Khi gợi ý, hãy đề cập tên khóa học, lý do phù hợp VÀ PHẢI bao gồm link đến khóa học.\n");
                prompt.append(
                        "📌 Format bắt buộc cho mỗi khóa học: Tên khóa học + mô tả + 🔗 [Xem khóa học](/courses/{course_id})\n");
            } else {
                prompt.append("(Không tìm thấy khóa học cụ thể trong database. ");
                prompt.append("Hãy tư vấn chung về chủ đề này và gợi ý hướng học tập phù hợp.)\n\n");
            }

        } else {
            // GENERAL Mode: Pure knowledge chat
            prompt.append(chatbotProperties.getSystemPrompt());
        }

        return prompt.toString();
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
