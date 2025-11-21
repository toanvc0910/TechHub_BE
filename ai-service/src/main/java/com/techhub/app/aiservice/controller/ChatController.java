package com.techhub.app.aiservice.controller;

import com.techhub.app.aiservice.dto.request.ChatMessageRequest;
import com.techhub.app.aiservice.dto.response.ChatMessageResponse;
import com.techhub.app.aiservice.service.ChatOrchestrationService;
import com.techhub.app.commonservice.payload.GlobalResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;

@RestController
@RequestMapping("/api/ai/chat")
@Validated
@RequiredArgsConstructor
public class ChatController {

    private final ChatOrchestrationService chatOrchestrationService;

    @PostMapping("/messages")
    public ResponseEntity<GlobalResponse<ChatMessageResponse>> sendMessage(
            @Valid @RequestBody ChatMessageRequest request,
            HttpServletRequest servletRequest) {

        ChatMessageResponse response = chatOrchestrationService.sendMessage(request);
        return ResponseEntity.ok(
                GlobalResponse.success("Chat processed", response)
                        .withStatus("AI_CHAT")
                        .withPath(servletRequest.getRequestURI())
        );
    }
}
