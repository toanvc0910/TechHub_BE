package com.techhub.app.blogservice.websocket.controller;

import com.techhub.app.commonservice.websocket.dto.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.annotation.SendToUser;
import org.springframework.stereotype.Controller;

import java.security.Principal;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * WebSocket Controller cho Blog Comments.
 * Xử lý real-time comments qua STOMP.
 * 
 * Subscribe topics:
 * - /topic/blog/{blogId}/comments - Nhận comment updates cho blog cụ thể
 * 
 * Send destinations:
 * - /app/blog/{blogId}/comment - Gửi comment mới
 * - /app/blog/{blogId}/comment/{commentId}/edit - Sửa comment
 * - /app/blog/{blogId}/comment/{commentId}/delete - Xóa comment
 */
@Slf4j
@Controller
@RequiredArgsConstructor
public class CommentWebSocketController {

    private final SimpMessagingTemplate messagingTemplate;

    /**
     * Xử lý khi client gửi comment mới qua WebSocket.
     * Client gửi tới: /app/blog/{blogId}/comment
     * Server broadcast tới: /topic/blog/{blogId}/comments
     */
    @MessageMapping("/blog/{blogId}/comment")
    public void handleNewComment(
            @DestinationVariable Long blogId,
            @Payload CommentPayload payload,
            Principal principal) {

        log.info(">>> [WS] Received message at /app/blog/{}/comment", blogId);
        log.info(">>> [WS] Payload: {}", payload);
        log.info(">>> [WS] Principal: {}", principal);

        String userName = "Anonymous";
        UUID userId = UUID.randomUUID();
        
        if (principal != null) {
            try {
                userId = UUID.fromString(principal.getName());
                userName = "User " + userId.toString().substring(0, 8);
            } catch (Exception e) {
                log.warn("Could not parse principal name as UUID: {}", principal.getName());
            }
        } else {
            log.warn("Unauthenticated user sending comment - allowing for testing");
        }

        log.info(">>> [WS] Processing comment for blog {} from {}", blogId, userName);

        // Tạo message để broadcast
        UUID commentId = UUID.randomUUID();
        
        CommentWebSocketMessage message = CommentWebSocketMessage.created(
                commentId,
                UUID.randomUUID(), // blogId as UUID for now - TODO: fix type
                CommentTargetType.BLOG,
                payload.getParentId(),
                userId,
                userName,
                null, // avatarUrl
                payload.getContent(),
                OffsetDateTime.now()
        );

        // Broadcast tới tất cả subscribers
        String destination = "/topic/blog/" + blogId + "/comments";
        log.info(">>> [WS] Broadcasting to: {}", destination);
        messagingTemplate.convertAndSend(destination, message);
        log.info(">>> [WS] Broadcast completed!");
    }

    /**
     * Xử lý khi client sửa comment.
     * Client gửi tới: /app/blog/{blogId}/comment/{commentId}/edit
     * Server broadcast tới: /topic/blog/{blogId}/comments
     */
    @MessageMapping("/blog/{blogId}/comment/{commentId}/edit")
    public void handleEditComment(
            @DestinationVariable Long blogId,
            @DestinationVariable Long commentId,
            @Payload CommentPayload payload,
            Principal principal) {

        log.info(">>> [WS] Edit comment {} for blog {}", commentId, blogId);

        UUID userId = UUID.randomUUID();
        if (principal != null) {
            try {
                userId = UUID.fromString(principal.getName());
            } catch (Exception e) {
                log.warn("Could not parse principal: {}", principal.getName());
            }
        }

        CommentWebSocketMessage message = CommentWebSocketMessage.updated(
                UUID.randomUUID(), // commentId
                UUID.randomUUID(), // targetId
                CommentTargetType.BLOG,
                userId,
                payload.getContent()
        );

        String destination = "/topic/blog/" + blogId + "/comments";
        messagingTemplate.convertAndSend(destination, message);
        log.info(">>> [WS] Broadcasted edit to {}", destination);
    }

    /**
     * Xử lý khi client xóa comment.
     * Client gửi tới: /app/blog/{blogId}/comment/{commentId}/delete
     * Server broadcast tới: /topic/blog/{blogId}/comments
     */
    @MessageMapping("/blog/{blogId}/comment/{commentId}/delete")
    public void handleDeleteComment(
            @DestinationVariable Long blogId,
            @DestinationVariable Long commentId,
            Principal principal) {

        log.info(">>> [WS] Delete comment {} for blog {}", commentId, blogId);

        UUID userId = UUID.randomUUID();
        if (principal != null) {
            try {
                userId = UUID.fromString(principal.getName());
            } catch (Exception e) {
                log.warn("Could not parse principal: {}", principal.getName());
            }
        }

        CommentWebSocketMessage message = CommentWebSocketMessage.deleted(
                UUID.randomUUID(), // commentId
                UUID.randomUUID(), // targetId
                CommentTargetType.BLOG,
                userId
        );

        String destination = "/topic/blog/" + blogId + "/comments";
        messagingTemplate.convertAndSend(destination, message);
        log.info(">>> [WS] Broadcasted delete to {}", destination);
    }

    /**
     * Error handler - gửi error message đến user cụ thể.
     */
    @MessageMapping("/error")
    @SendToUser("/queue/errors")
    public WebSocketErrorResponse handleError(Exception e, Principal principal) {
        log.error("WebSocket error for user {}: {}", 
                principal != null ? principal.getName() : "anonymous", e.getMessage());
        return WebSocketErrorResponse.internalError(e.getMessage());
    }

    /**
     * Gửi reply notification đến author của parent comment.
     * Gọi phương thức này khi có reply mới.
     */
    private void sendReplyNotification(UUID authorId, CommentWebSocketMessage message) {
        String destination = "/user/" + authorId + "/queue/comments";
        messagingTemplate.convertAndSend(destination, message);
        log.debug("Sent reply notification to user {}", authorId);
    }
}
