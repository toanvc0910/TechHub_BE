package com.techhub.app.aiservice.service;

import com.techhub.app.aiservice.config.ChatbotProperties;
import com.techhub.app.aiservice.dto.request.ChatMessageRequest;
import com.techhub.app.aiservice.entity.ChatMessage;
import com.techhub.app.aiservice.entity.ChatSession;
import com.techhub.app.aiservice.enums.ChatMode;
import com.techhub.app.aiservice.enums.ChatSender;
import com.techhub.app.aiservice.exception.RateLimitExceededException;
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
    private final SwitchableAiGateway aiGateway;
    private final ChatbotProperties chatbotProperties;
    private final VectorService vectorService;
    private final PromptSanitizationService sanitizationService;
    private final RateLimitingService rateLimitingService;

    /**
     * Send a streaming chat message and return Flux of response chunks
     */
    @Transactional
    public Flux<String> sendStreamingMessage(ChatMessageRequest request) {
        log.info("📨 [ChatStreamingService] ===== PROCESSING STREAMING MESSAGE =====");
        log.info("📨 [ChatStreamingService] User: {}, Session: {}, Mode: {}",
                request.getUserId(), request.getSessionId(), request.getMode());
        log.info("📨 [ChatStreamingService] Message: {}", request.getMessage());

        // 1. Check rate limiting
        if (!rateLimitingService.isAllowed(request.getUserId())) {
            int remainingPerMin = rateLimitingService.getRemainingRequestsPerMinute(request.getUserId());
            int remainingPerHour = rateLimitingService.getRemainingRequestsPerHour(request.getUserId());
            log.warn("Rate limit exceeded for user {} in streaming mode", request.getUserId());
            return Flux.error(new RateLimitExceededException(
                    "Too many requests. Please try again later.",
                    remainingPerMin,
                    remainingPerHour));
        }

        // 2. Sanitize user input
        String sanitizedMessage;
        try {
            sanitizedMessage = sanitizationService.sanitize(request.getMessage());
            log.debug("Message sanitized for streaming request");
        } catch (IllegalArgumentException e) {
            log.warn("Prompt injection detected in streaming request: {}", e.getMessage());
            return Flux.error(e);
        }

        // Load or create session
        ChatSession session = loadOrCreateSession(request);
        log.info("📨 [ChatStreamingService] Session loaded/created: {}", session.getId());

        // Save user message with sanitized content
        ChatMessage userMessage = new ChatMessage();
        userMessage.setSession(session);
        userMessage.setSender(ChatSender.USER);
        userMessage.setContent(sanitizedMessage);
        chatMessageRepository.save(userMessage);
        log.info("📨 [ChatStreamingService] User message saved");

        // Build messages list for OpenAI with embedding context
        List<Map<String, String>> messages = buildMessageHistoryWithContext(session, request, sanitizedMessage);
        log.info("📨 [ChatStreamingService] Message history built with context, {} messages", messages.size());

        // Accumulate response for saving
        StringBuilder fullResponse = new StringBuilder();

        log.info("📨 [ChatStreamingService] Calling SwitchableAiGateway.generateStreamingResponseWithHistory...");
        return aiGateway.generateStreamingResponseWithHistory(messages)
                .doOnNext(chunk -> {
                    log.info("📦 [ChatStreamingService] Received chunk from Gemini: {}", chunk);
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

        return aiGateway.generateStreamingResponse(message, chatbotProperties.getSystemPrompt());
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
    private List<Map<String, String>> buildMessageHistoryWithContext(ChatSession session, ChatMessageRequest request,
            String sanitizedMessage) {
        List<Map<String, String>> messages = new ArrayList<>();

        // Build system prompt with context based on mode
        String systemPrompt = buildSystemPromptWithContext(request, sanitizedMessage);
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
    private String buildSystemPromptWithContext(ChatMessageRequest request, String sanitizedMessage) {
        StringBuilder prompt = new StringBuilder();

        if (request.getMode() == ChatMode.ADVISOR) {
            // ADVISOR Mode: Search Qdrant for relevant courses
            prompt.append("Bạn là cố vấn học tập thông minh của TechHub - nền tảng học lập trình online.\n");
            prompt.append(
                    "Nhiệm vụ: Dựa trên câu hỏi của người dùng và danh sách khóa học bên dưới, hãy gợi ý khóa học phù hợp.\n");
            prompt.append("Hãy trả lời thân thiện, chi tiết và đề xuất cụ thể các khóa học nếu có.\n\n");
            prompt.append(
                    "⚠️ QUAN TRỌNG: Khi gợi ý khóa học, BẮT BUỘC phải bao gồm link dạng markdown: [Xem khóa học](/courses/{course_id})\n\n");

            log.info("🔍 [ADVISOR MODE - Streaming] Searching Qdrant for: {}", sanitizedMessage);

            List<Map<String, Object>> relevantCourses = null;
            try {
                relevantCourses = vectorService.searchCourses(sanitizedMessage, 5);
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
                        // Sanitize database content
                        String title = sanitizeDbContent(String.valueOf(payload.get("title")));
                        String description = sanitizeDbContent(String.valueOf(payload.get("description")));
                        String level = sanitizeDbContent(String.valueOf(payload.get("level")));
                        Object courseId = payload.get("id");

                        prompt.append(String.format("**%d. %s**\n", ++count, title));
                        prompt.append("   - Mô tả: ").append(description).append("\n");
                        prompt.append("   - Trình độ: ").append(level).append("\n");
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

    /**
     * Sanitize database content to prevent indirect prompt injection
     */
    private String sanitizeDbContent(String content) {
        if (content == null || content.equals("null")) {
            return "";
        }
        // Remove control characters and normalize whitespace
        return content.replaceAll("[\\p{Cntrl}&&[^\r\n\t]]", "")
                .replaceAll("\n{3,}", "\n\n")
                .trim();
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
