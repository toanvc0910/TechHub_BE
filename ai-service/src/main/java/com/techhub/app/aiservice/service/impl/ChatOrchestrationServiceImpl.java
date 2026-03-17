package com.techhub.app.aiservice.service.impl;

import com.techhub.app.aiservice.dto.request.ChatMessageRequest;
import com.techhub.app.aiservice.dto.response.ChatMessageResponse;
import com.techhub.app.aiservice.dto.response.ChatSessionResponse;
import com.techhub.app.aiservice.entity.ChatMessage;
import com.techhub.app.aiservice.entity.ChatSession;
import com.techhub.app.aiservice.enums.ChatMode;
import com.techhub.app.aiservice.enums.ChatSender;
import com.techhub.app.aiservice.exception.RateLimitExceededException;
import com.techhub.app.aiservice.repository.ChatMessageRepository;
import com.techhub.app.aiservice.repository.ChatSessionRepository;
import com.techhub.app.aiservice.service.ChatOrchestrationService;
import com.techhub.app.aiservice.service.SwitchableAiGateway;
import com.techhub.app.aiservice.service.PromptSanitizationService;
import com.techhub.app.aiservice.service.RateLimitingService;
import com.techhub.app.aiservice.service.VectorService;
import com.techhub.app.commonservice.exception.NotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class ChatOrchestrationServiceImpl implements ChatOrchestrationService {

    private final ChatSessionRepository chatSessionRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final SwitchableAiGateway aiGateway;
    private final VectorService vectorService;
    private final PromptSanitizationService sanitizationService;
    private final RateLimitingService rateLimitingService;

    @Override
    @Transactional
    public ChatMessageResponse sendMessage(ChatMessageRequest request) {
        // 1. Check rate limiting first
        if (!rateLimitingService.isAllowed(request.getUserId())) {
            int remainingPerMin = rateLimitingService.getRemainingRequestsPerMinute(request.getUserId());
            int remainingPerHour = rateLimitingService.getRemainingRequestsPerHour(request.getUserId());
            log.warn("Rate limit exceeded for user {}", request.getUserId());
            throw new RateLimitExceededException(
                    "Too many requests. Please try again later.",
                    remainingPerMin,
                    remainingPerHour);
        }

        // 2. Sanitize user input (additional layer beyond @SafePrompt validation)
        String sanitizedMessage = sanitizationService.sanitize(request.getMessage());
        log.debug("Message sanitized for user {}", request.getUserId());

        ChatSession session = loadOrCreateSession(request);

        ChatMessage userMessage = new ChatMessage();
        userMessage.setSession(session);
        userMessage.setSender(ChatSender.USER);
        userMessage.setContent(sanitizedMessage); // Use sanitized message
        chatMessageRepository.save(userMessage);

        // 3. Build prompt with sanitized input
        String prompt = buildPrompt(sanitizedMessage, request.getMode(), session);
        Object aiResponse = aiGateway.generateStructuredJson(prompt, request.getContext());

        // Extract clean text from OpenAI response
        String cleanAnswer = extractTextFromResponse(aiResponse);

        ChatMessage botMessage = new ChatMessage();
        botMessage.setSession(session);
        botMessage.setSender(ChatSender.BOT);
        botMessage.setContent(cleanAnswer);
        chatMessageRepository.save(botMessage);

        return ChatMessageResponse.builder()
                .sessionId(session.getId())
                .messageId(botMessage.getId())
                .mode(request.getMode())
                .message(cleanAnswer) // Use unified field
                .answer(cleanAnswer) // Keep for backward compatibility
                .context(null)
                .build();
    }

    /**
     * Extract clean text from OpenAI response object
     */
    @SuppressWarnings("unchecked")
    private String extractTextFromResponse(Object aiResponse) {
        if (aiResponse == null) {
            log.warn("AI response is null");
            return "Xin lỗi, tôi không thể tạo câu trả lời lúc này.";
        }

        try {
            if (aiResponse instanceof Map) {
                Map<String, Object> responseMap = (Map<String, Object>) aiResponse;

                log.debug("Response map keys: {}", responseMap.keySet());

                // Check for error first
                if (responseMap.containsKey("error")) {
                    log.error("OpenAI error: {}", responseMap.get("error"));
                    return "Xin lỗi, đã có lỗi xảy ra khi xử lý yêu cầu của bạn.";
                }

                String content = null;

                // Extract from OpenAI standard response structure
                if (responseMap.containsKey("choices")) {
                    List<Map<String, Object>> choices = (List<Map<String, Object>>) responseMap.get("choices");
                    if (!choices.isEmpty()) {
                        Map<String, Object> firstChoice = choices.get(0);
                        if (firstChoice.containsKey("message")) {
                            Map<String, Object> message = (Map<String, Object>) firstChoice.get("message");
                            if (message.containsKey("content")) {
                                content = String.valueOf(message.get("content"));
                                log.debug("Extracted content from choices[0].message.content");
                            }
                        }
                    }
                }

                // Fallback: check for direct message field (stub mode)
                if (content == null && responseMap.containsKey("message")) {
                    content = String.valueOf(responseMap.get("message"));
                    log.debug("Extracted content from direct message field");
                }

                // If response contains "response" field with nested structure
                if (content == null && responseMap.containsKey("response")) {
                    Object nestedResponse = responseMap.get("response");
                    if (nestedResponse instanceof Map) {
                        Map<String, Object> nested = (Map<String, Object>) nestedResponse;
                        if (nested.containsKey("message")) {
                            content = String.valueOf(nested.get("message"));
                            log.debug("Extracted content from response.message");
                        }
                    }
                }

                // If we got content, try to parse and format it
                if (content != null && !content.isEmpty() && !content.equals("null")) {
                    log.info("Successfully extracted content, length: {}", content.length());
                    return formatJsonResponse(content);
                } else {
                    log.warn("Content is null or empty after extraction. Response: {}", responseMap);
                }
            }

            // Last resort: convert to string
            String fallback = aiResponse.toString();
            log.warn("Using fallback toString, length: {}", fallback.length());
            if (fallback.length() > 1000) {
                log.warn("Response too long or malformed");
                return "Xin lỗi, câu trả lời quá dài hoặc có định dạng không đúng.";
            }
            return formatJsonResponse(fallback);

        } catch (Exception e) {
            log.error("Failed to extract text from AI response: {}", e.getMessage(), e);
            return "Xin lỗi, đã có lỗi khi xử lý câu trả lời.";
        }
    }

    /**
     * Format JSON response to human-readable text
     */
    @SuppressWarnings("unchecked")
    private String formatJsonResponse(String jsonContent) {
        try {
            // Try to parse as JSON
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            Map<String, Object> jsonMap = mapper.readValue(jsonContent, Map.class);

            StringBuilder formatted = new StringBuilder();

            // Check for message field first
            if (jsonMap.containsKey("message")) {
                formatted.append(jsonMap.get("message")).append("\n\n");
            }

            // Check for response object
            if (jsonMap.containsKey("response")) {
                Object resp = jsonMap.get("response");
                if (resp instanceof Map) {
                    Map<String, Object> respMap = (Map<String, Object>) resp;
                    formatted.append(formatResponseObject(respMap));
                }
            }

            // Check for suggestions/courses
            if (jsonMap.containsKey("suggestions")) {
                formatted.append(formatCourses(jsonMap.get("suggestions")));
            }

            // Check for definition (general chat)
            if (jsonMap.containsKey("definition")) {
                formatted.append("📖 **Định nghĩa:**\n").append(jsonMap.get("definition")).append("\n\n");
            }

            if (jsonMap.containsKey("purpose")) {
                formatted.append("🎯 **Mục đích:**\n").append(jsonMap.get("purpose")).append("\n\n");
            }

            if (jsonMap.containsKey("languages")) {
                formatted.append("💻 **Ngôn ngữ lập trình:**\n");
                List<String> langs = (List<String>) jsonMap.get("languages");
                for (String lang : langs) {
                    formatted.append("  • ").append(lang).append("\n");
                }
                formatted.append("\n");
            }

            if (jsonMap.containsKey("key_concepts")) {
                formatted.append("🔑 **Khái niệm chính:**\n");
                List<String> concepts = (List<String>) jsonMap.get("key_concepts");
                for (String concept : concepts) {
                    formatted.append("  • ").append(concept).append("\n");
                }
            }

            return formatted.toString().trim();

        } catch (Exception e) {
            // Not valid JSON, return as is
            log.debug("Content is not JSON, returning as plain text");
            return jsonContent;
        }
    }

    @SuppressWarnings("unchecked")
    private String formatResponseObject(Map<String, Object> respMap) {
        StringBuilder sb = new StringBuilder();

        if (respMap.containsKey("definition")) {
            sb.append("📖 ").append(respMap.get("definition")).append("\n\n");
        }
        if (respMap.containsKey("purpose")) {
            sb.append("🎯 ").append(respMap.get("purpose")).append("\n\n");
        }
        if (respMap.containsKey("languages")) {
            sb.append("💻 **Ngôn ngữ:**\n");
            List<String> langs = (List<String>) respMap.get("languages");
            for (String lang : langs) {
                sb.append("  • ").append(lang).append("\n");
            }
            sb.append("\n");
        }
        if (respMap.containsKey("key_concepts")) {
            sb.append("🔑 **Khái niệm chính:**\n");
            List<String> concepts = (List<String>) respMap.get("key_concepts");
            for (String concept : concepts) {
                sb.append("  • ").append(concept).append("\n");
            }
        }

        return sb.toString();
    }

    @SuppressWarnings("unchecked")
    private String formatCourses(Object suggestions) {
        StringBuilder sb = new StringBuilder();

        if (suggestions instanceof List) {
            List<Map<String, Object>> courses = (List<Map<String, Object>>) suggestions;

            if (!courses.isEmpty()) {
                sb.append("📚 **Các khóa học gợi ý:**\n\n");

                int index = 1;
                for (Map<String, Object> course : courses) {
                    String courseName = (String) course.get("course_name");
                    String courseId = (String) course.get("id"); // Changed from course_id to id
                    String description = (String) course.get("description");
                    String level = (String) course.get("level");

                    sb.append(index++).append(". **").append(courseName).append("**\n");

                    if (description != null && !description.equals("null")) {
                        sb.append("   📝 ").append(description).append("\n");
                    }
                    if (level != null && !level.equals("null")) {
                        sb.append("   📊 Trình độ: ").append(level).append("\n");
                    }
                    if (courseId != null) {
                        // Return as clickable link format that FE can parse
                        sb.append("   🔗 [Xem khóa học](/courses/").append(courseId).append(")\n");
                    }
                    sb.append("\n");
                }
            }
        }

        return sb.toString();
    }

    private ChatSession loadOrCreateSession(ChatMessageRequest request) {
        if (request.getSessionId() != null) {
            return chatSessionRepository.findByIdAndUserId(request.getSessionId(), request.getUserId())
                    .orElseThrow(() -> new NotFoundException("Session not found for user"));
        }

        ChatSession session = new ChatSession();
        session.setUserId(request.getUserId());
        session.setContext(request.getContext());
        session.setStartedAt(OffsetDateTime.now());
        return chatSessionRepository.save(session);
    }

    private String buildPrompt(String sanitizedMessage, ChatMode mode, ChatSession session) {
        StringBuilder prompt = new StringBuilder();

        if (mode == ChatMode.ADVISOR) {
            // Advisor Mode: Search Qdrant for relevant courses
            prompt.append("Bạn là cố vấn học tập thông minh của TechHub.\n");
            prompt.append(
                    "Nhiệm vụ: Dựa trên câu hỏi của người dùng và danh sách khóa học, hãy gợi ý khóa học phù hợp.\n");
            prompt.append(
                    "Format response: Trả về JSON với cấu trúc {\"message\": \"...\", \"suggestions\": [{\"course_name\": \"...\", \"course_id\": \"...\", \"description\": \"...\", \"level\": \"...\"}]}\n\n");

            log.info("🔍 [ADVISOR MODE] Searching Qdrant for: {}", sanitizedMessage);

            List<Map<String, Object>> relevantCourses = null;
            try {
                relevantCourses = vectorService.searchCourses(sanitizedMessage, 5);
            } catch (Exception e) {
                log.error("Failed to search courses from Qdrant: {}", e.getMessage(), e);
                relevantCourses = List.of();
            }

            if (relevantCourses != null && !relevantCourses.isEmpty()) {
                prompt.append("=== Các khóa học liên quan từ database ===\n");
                int count = 0;
                for (Map<String, Object> course : relevantCourses) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> payload = (Map<String, Object>) course.get("payload");
                    if (payload != null) {
                        // Sanitize database content as well
                        String title = sanitizeDbContent(String.valueOf(payload.get("title")));
                        String description = sanitizeDbContent(String.valueOf(payload.get("description")));
                        String level = sanitizeDbContent(String.valueOf(payload.get("level")));
                        String id = sanitizeDbContent(String.valueOf(payload.get("id")));

                        prompt.append(String.format("%d. %s\n", ++count, title));
                        prompt.append("   Mô tả: ").append(description).append("\n");
                        prompt.append("   Trình độ: ").append(level).append("\n");
                        prompt.append("   Course ID: ").append(id).append("\n\n");
                    }
                }
            } else {
                prompt.append("(Không tìm thấy khóa học phù hợp trong database. Hãy tư vấn chung về chủ đề này.)\n\n");
            }

            prompt.append("Câu hỏi của user: ").append(sanitizedMessage).append("\n\n");
            prompt.append("Hãy trả lời thân thiện và gợi ý khóa học cụ thể nếu có (bao gồm course_name và course_id).");

        } else {
            // General Mode: Pure knowledge chat
            prompt.append("Bạn là trợ lý AI thông minh giải đáp kiến thức lập trình và công nghệ.\n");
            prompt.append(
                    "Format response: Trả về JSON với cấu trúc có \"response\" object chứa thông tin chi tiết.\n");
            prompt.append(
                    "Ví dụ: {\"response\": {\"definition\": \"...\", \"purpose\": \"...\", \"languages\": [...], \"key_concepts\": [...]}}\n\n");
            prompt.append("Câu hỏi: ").append(sanitizedMessage).append("\n\n");
            prompt.append("Hãy trả lời chi tiết, có cấu trúc và dễ hiểu.");
        }

        log.debug("Built prompt for mode {}: {}", mode, prompt.toString());
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

    @Override
    @Transactional
    public ChatSessionResponse createSession(UUID userId, ChatMode mode) {
        ChatSession session = new ChatSession();
        session.setUserId(userId);
        session.setStartedAt(OffsetDateTime.now());

        // Set context with mode if provided
        if (mode != null) {
            Map<String, Object> context = new HashMap<>();
            context.put("mode", mode.toString());
            session.setContext(context);
        }

        ChatSession savedSession = chatSessionRepository.save(session);
        log.info("✅ Created new empty session: {} for user: {}", savedSession.getId(), userId);

        return ChatSessionResponse.builder()
                .id(savedSession.getId())
                .userId(savedSession.getUserId())
                .startedAt(savedSession.getStartedAt())
                .endedAt(savedSession.getEndedAt())
                .context(savedSession.getContext())
                .build();
    }

    @Override
    @Transactional
    public void deleteSession(UUID sessionId, UUID userId) {
        ChatSession session = chatSessionRepository.findByIdAndUserId(sessionId, userId)
                .orElseThrow(() -> new NotFoundException("Session not found or unauthorized"));

        // Delete will cascade to messages via database constraint
        chatSessionRepository.delete(session);
        log.info("Deleted session {} for user {}", sessionId, userId);
    }

    @Override
    @Transactional
    public void cleanupOldSessions(int daysToKeep) {
        LocalDateTime cutoffDate = LocalDateTime.now().minusDays(daysToKeep);
        chatSessionRepository.deleteOldSessions(cutoffDate);
        log.info("Cleaned up sessions older than {} days (cutoff: {})", daysToKeep, cutoffDate);
    }
}
