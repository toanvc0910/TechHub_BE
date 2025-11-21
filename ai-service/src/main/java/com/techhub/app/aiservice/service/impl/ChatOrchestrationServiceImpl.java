package com.techhub.app.aiservice.service.impl;

import com.techhub.app.aiservice.dto.request.ChatMessageRequest;
import com.techhub.app.aiservice.dto.response.ChatMessageResponse;
import com.techhub.app.aiservice.entity.ChatMessage;
import com.techhub.app.aiservice.entity.ChatSession;
import com.techhub.app.aiservice.enums.ChatMode;
import com.techhub.app.aiservice.enums.ChatSender;
import com.techhub.app.aiservice.repository.ChatMessageRepository;
import com.techhub.app.aiservice.repository.ChatSessionRepository;
import com.techhub.app.aiservice.service.ChatOrchestrationService;
import com.techhub.app.aiservice.service.OpenAiGateway;
import com.techhub.app.aiservice.service.VectorService;
import com.techhub.app.commonservice.exception.NotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class ChatOrchestrationServiceImpl implements ChatOrchestrationService {

    private final ChatSessionRepository chatSessionRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final OpenAiGateway openAiGateway;
    private final VectorService vectorService;

    @Override
    @Transactional
    public ChatMessageResponse sendMessage(ChatMessageRequest request) {
        ChatSession session = loadOrCreateSession(request);

        ChatMessage userMessage = new ChatMessage();
        userMessage.setSession(session);
        userMessage.setSender(ChatSender.USER);
        userMessage.setContent(request.getMessage());
        chatMessageRepository.save(userMessage);

        String prompt = buildPrompt(request, session);
        Object aiResponse = openAiGateway.generateStructuredJson(prompt, request.getContext());
        String botMessageContent = aiResponse != null ? aiResponse.toString() : "No response";

        ChatMessage botMessage = new ChatMessage();
        botMessage.setSession(session);
        botMessage.setSender(ChatSender.BOT);
        botMessage.setContent(botMessageContent);
        chatMessageRepository.save(botMessage);

        return ChatMessageResponse.builder()
                .sessionId(session.getId())
                .messageId(botMessage.getId())
                .mode(request.getMode())
                .answer(botMessage.getContent())
                .context(aiResponse)
                .build();
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

    private String buildPrompt(ChatMessageRequest request, ChatSession session) {
        StringBuilder prompt = new StringBuilder();

        if (request.getMode() == ChatMode.ADVISOR) {
            // Advisor Mode: Search Qdrant for relevant courses
            prompt.append("Bạn là cố vấn học tập thông minh của TechHub.\n");
            prompt.append("Dựa trên câu hỏi của người dùng, hãy gợi ý khóa học phù hợp từ database.\n\n");

            log.info("🔍 [ADVISOR MODE] Searching Qdrant for: {}", request.getMessage());
            List<Map<String, Object>> relevantCourses = vectorService.searchCourses(request.getMessage(), 5);

            if (!relevantCourses.isEmpty()) {
                prompt.append("=== Các khóa học liên quan từ database ===\n");
                int count = 0;
                for (Map<String, Object> course : relevantCourses) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> payload = (Map<String, Object>) course.get("payload");
                    if (payload != null) {
                        prompt.append(String.format("%d. %s\n", ++count, payload.get("title")));
                        prompt.append("   Mô tả: ").append(payload.get("description")).append("\n");
                        prompt.append("   Trình độ: ").append(payload.get("level")).append("\n");
                        prompt.append("   Course ID: ").append(payload.get("course_id")).append("\n\n");
                    }
                }
            } else {
                prompt.append("(Không tìm thấy khóa học phù hợp trong database)\n\n");
            }

            prompt.append("Context: ").append(session.getContext()).append("\n");
            prompt.append("Câu hỏi của user: ").append(request.getMessage()).append("\n\n");
            prompt.append("Hãy trả lời dựa trên thông tin trên và gợi ý khóa học cụ thể (bao gồm Course ID).");

        } else {
            // General Mode: Pure knowledge chat
            prompt.append("Bạn là trợ lý giải đáp kiến thức lập trình chung.\n");
            prompt.append("Context: ").append(session.getContext()).append("\n");
            prompt.append("Câu hỏi: ").append(request.getMessage());
        }

        return prompt.toString();
    }
}
