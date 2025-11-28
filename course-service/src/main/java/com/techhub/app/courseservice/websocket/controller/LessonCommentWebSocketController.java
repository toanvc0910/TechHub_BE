package com.techhub.app.courseservice.websocket.controller;

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
 * WebSocket Controller cho Lesson Comments.
 * Xử lý real-time comments qua STOMP cho Lesson trong Course.
 * 
 * Subscribe topics:
 * - /topic/lesson/{lessonId}/comments - Nhận comment updates cho lesson cụ thể
 * 
 * Send destinations:
 * - /app/lesson/{lessonId}/comment - Gửi comment mới
 * - /app/lesson/{lessonId}/comment/{commentId}/edit - Sửa comment
 * - /app/lesson/{lessonId}/comment/{commentId}/delete - Xóa comment
 */
@Slf4j
@Controller
@RequiredArgsConstructor
public class LessonCommentWebSocketController {

    private final SimpMessagingTemplate messagingTemplate;

    /**
     * Xử lý khi client gửi comment mới cho Lesson qua WebSocket.
     * Client gửi tới: /app/lesson/{lessonId}/comment
     * Server broadcast tới: /topic/lesson/{lessonId}/comments
     */
    @MessageMapping("/lesson/{lessonId}/comment")
    public void handleNewComment(
            @DestinationVariable UUID lessonId,
            @Payload CommentPayload payload,
            Principal principal) {

        if (principal == null) {
            log.warn("Unauthenticated user tried to send comment");
            return;
        }

        UUID userId = UUID.fromString(principal.getName());
        log.info("Received new comment for lesson {} from user {}", lessonId, userId);

        // TODO: Gọi CommentService để lưu comment vào database
        // Comment savedComment = commentService.createLessonComment(lessonId, userId, payload);

        UUID commentId = UUID.randomUUID(); // Sẽ lấy từ savedComment.getId()
        
        CommentWebSocketMessage message = CommentWebSocketMessage.created(
                commentId,
                lessonId,
                CommentTargetType.LESSON,
                payload.getParentId(),
                userId,
                "User " + userId.toString().substring(0, 8),
                null,
                payload.getContent(),
                OffsetDateTime.now()
        );

        String destination = "/topic/lesson/" + lessonId + "/comments";
        messagingTemplate.convertAndSend(destination, message);
        log.debug("Broadcasted comment to {}", destination);

        // Nếu là reply, gửi notification tới author của parent comment
        if (payload.getParentId() != null) {
            // TODO: Lấy authorId của parent comment từ database
            // sendReplyNotification(parentAuthorId, message);
        }
    }

    /**
     * Xử lý khi client sửa comment.
     */
    @MessageMapping("/lesson/{lessonId}/comment/{commentId}/edit")
    public void handleEditComment(
            @DestinationVariable UUID lessonId,
            @DestinationVariable UUID commentId,
            @Payload CommentPayload payload,
            Principal principal) {

        if (principal == null) {
            log.warn("Unauthenticated user tried to edit comment");
            return;
        }

        UUID userId = UUID.fromString(principal.getName());
        log.info("Editing comment {} for lesson {} by user {}", commentId, lessonId, userId);

        CommentWebSocketMessage message = CommentWebSocketMessage.updated(
                commentId,
                lessonId,
                CommentTargetType.LESSON,
                userId,
                payload.getContent()
        );

        String destination = "/topic/lesson/" + lessonId + "/comments";
        messagingTemplate.convertAndSend(destination, message);
    }

    /**
     * Xử lý khi client xóa comment.
     */
    @MessageMapping("/lesson/{lessonId}/comment/{commentId}/delete")
    public void handleDeleteComment(
            @DestinationVariable UUID lessonId,
            @DestinationVariable UUID commentId,
            Principal principal) {

        if (principal == null) {
            log.warn("Unauthenticated user tried to delete comment");
            return;
        }

        UUID userId = UUID.fromString(principal.getName());
        log.info("Deleting comment {} for lesson {} by user {}", commentId, lessonId, userId);

        CommentWebSocketMessage message = CommentWebSocketMessage.deleted(
                commentId,
                lessonId,
                CommentTargetType.LESSON,
                userId
        );

        String destination = "/topic/lesson/" + lessonId + "/comments";
        messagingTemplate.convertAndSend(destination, message);
    }

    /**
     * Error handler
     */
    @MessageMapping("/lesson/error")
    @SendToUser("/queue/errors")
    public WebSocketErrorResponse handleError(Exception e, Principal principal) {
        log.error("WebSocket error for user {}: {}", 
                principal != null ? principal.getName() : "anonymous", e.getMessage());
        return WebSocketErrorResponse.internalError(e.getMessage());
    }

    private void sendReplyNotification(UUID authorId, CommentWebSocketMessage message) {
        String destination = "/user/" + authorId + "/queue/comments";
        messagingTemplate.convertAndSend(destination, message);
        log.debug("Sent reply notification to user {}", authorId);
    }
}
