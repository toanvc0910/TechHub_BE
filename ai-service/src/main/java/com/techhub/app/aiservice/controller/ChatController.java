package com.techhub.app.aiservice.controller;

import com.techhub.app.aiservice.dto.request.ChatMessageRequest;
import com.techhub.app.aiservice.dto.response.ChatMessageResponse;
import com.techhub.app.aiservice.dto.response.ChatSessionResponse;
import com.techhub.app.aiservice.dto.response.ChatMessageDetailResponse;
import com.techhub.app.aiservice.entity.ChatSession;
import com.techhub.app.aiservice.entity.ChatMessage;
import com.techhub.app.aiservice.repository.ChatSessionRepository;
import com.techhub.app.aiservice.repository.ChatMessageRepository;
import com.techhub.app.aiservice.service.ChatOrchestrationService;
import com.techhub.app.commonservice.payload.GlobalResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/ai/chat")
@Validated
@RequiredArgsConstructor
public class ChatController {

    private final ChatOrchestrationService chatOrchestrationService;
    private final ChatSessionRepository chatSessionRepository;
    private final ChatMessageRepository chatMessageRepository;

    @PostMapping("/messages")
    public ResponseEntity<GlobalResponse<ChatMessageResponse>> sendMessage(
            @Valid @RequestBody ChatMessageRequest request,
            HttpServletRequest servletRequest) {

        ChatMessageResponse response = chatOrchestrationService.sendMessage(request);
        return ResponseEntity.ok(
                GlobalResponse.success("Chat processed", response)
                        .withStatus("AI_CHAT")
                        .withPath(servletRequest.getRequestURI()));
    }

    @GetMapping("/sessions")
    public ResponseEntity<GlobalResponse<List<ChatSessionResponse>>> getUserSessions(
            @RequestParam UUID userId,
            HttpServletRequest servletRequest) {

        List<ChatSession> sessions = chatSessionRepository.findByUserIdOrderByStartedAtDesc(userId);
        List<ChatSessionResponse> responses = sessions.stream()
                .map(session -> ChatSessionResponse.builder()
                        .id(session.getId())
                        .userId(session.getUserId())
                        .startedAt(session.getStartedAt())
                        .endedAt(session.getEndedAt())
                        .context(session.getContext())
                        .build())
                .collect(Collectors.toList());

        return ResponseEntity.ok(
                GlobalResponse.success("User sessions retrieved", responses)
                        .withStatus("SUCCESS")
                        .withPath(servletRequest.getRequestURI()));
    }

    @GetMapping("/sessions/{sessionId}/messages")
    public ResponseEntity<GlobalResponse<List<ChatMessageDetailResponse>>> getSessionMessages(
            @PathVariable UUID sessionId,
            HttpServletRequest servletRequest) {

        ChatSession session = chatSessionRepository.findById(sessionId)
                .orElseThrow(() -> new RuntimeException("Session not found"));

        List<ChatMessage> messages = chatMessageRepository.findBySessionOrderByTimestampAsc(session);
        List<ChatMessageDetailResponse> responses = messages.stream()
                .map(message -> ChatMessageDetailResponse.builder()
                        .id(message.getId())
                        .sessionId(message.getSession().getId())
                        .sender(message.getSender())
                        .content(message.getContent())
                        .timestamp(message.getTimestamp())
                        .build())
                .collect(Collectors.toList());

        return ResponseEntity.ok(
                GlobalResponse.success("Session messages retrieved", responses)
                        .withStatus("SUCCESS")
                        .withPath(servletRequest.getRequestURI()));
    }

    @DeleteMapping("/sessions/{sessionId}")
    public ResponseEntity<GlobalResponse<Void>> deleteSession(
            @PathVariable UUID sessionId,
            @RequestParam UUID userId,
            HttpServletRequest servletRequest) {

        chatOrchestrationService.deleteSession(sessionId, userId);
        return ResponseEntity.ok(
                GlobalResponse.<Void>success("Session deleted successfully", null)
                        .withStatus("SUCCESS")
                        .withPath(servletRequest.getRequestURI()));
    }
}
