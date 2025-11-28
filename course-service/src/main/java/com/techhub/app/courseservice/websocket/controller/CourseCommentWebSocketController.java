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
 * WebSocket Controller cho Course Comments.
 * Xử lý real-time comments qua STOMP cho Course.
 * 
 * Subscribe topics:
 * - /topic/course/{courseId}/comments - Nhận comment updates cho course cụ thể
 * 
 * Send destinations:
 * - /app/course/{courseId}/comment - Gửi comment mới
 * - /app/course/{courseId}/comment/{commentId}/edit - Sửa comment
 * - /app/course/{courseId}/comment/{commentId}/delete - Xóa comment
 */
@Slf4j
@Controller
@RequiredArgsConstructor
public class CourseCommentWebSocketController {

    private final SimpMessagingTemplate messagingTemplate;

    /**
     * Xử lý khi client gửi comment mới cho Course qua WebSocket.
     * Client gửi tới: /app/course/{courseId}/comment
     * Server broadcast tới: /topic/course/{courseId}/comments
     */
    @MessageMapping("/course/{courseId}/comment")
    public void handleNewComment(
            @DestinationVariable UUID courseId,
            @Payload CommentPayload payload,
            Principal principal) {

        if (principal == null) {
            log.warn("Unauthenticated user tried to send comment");
            return;
        }

        UUID userId = UUID.fromString(principal.getName());
        log.info("Received new comment for course {} from user {}", courseId, userId);

        // TODO: Gọi CommentService để lưu comment vào database
        // Comment savedComment = commentService.createCourseComment(courseId, userId, payload);

        UUID commentId = UUID.randomUUID(); // Sẽ lấy từ savedComment.getId()
        
        CommentWebSocketMessage message = CommentWebSocketMessage.created(
                commentId,
                courseId,
                CommentTargetType.COURSE,
                payload.getParentId(),
                userId,
                "User " + userId.toString().substring(0, 8),
                null,
                payload.getContent(),
                OffsetDateTime.now()
        );

        String destination = "/topic/course/" + courseId + "/comments";
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
    @MessageMapping("/course/{courseId}/comment/{commentId}/edit")
    public void handleEditComment(
            @DestinationVariable UUID courseId,
            @DestinationVariable UUID commentId,
            @Payload CommentPayload payload,
            Principal principal) {

        if (principal == null) {
            log.warn("Unauthenticated user tried to edit comment");
            return;
        }

        UUID userId = UUID.fromString(principal.getName());
        log.info("Editing comment {} for course {} by user {}", commentId, courseId, userId);

        CommentWebSocketMessage message = CommentWebSocketMessage.updated(
                commentId,
                courseId,
                CommentTargetType.COURSE,
                userId,
                payload.getContent()
        );

        String destination = "/topic/course/" + courseId + "/comments";
        messagingTemplate.convertAndSend(destination, message);
    }

    /**
     * Xử lý khi client xóa comment.
     */
    @MessageMapping("/course/{courseId}/comment/{commentId}/delete")
    public void handleDeleteComment(
            @DestinationVariable UUID courseId,
            @DestinationVariable UUID commentId,
            Principal principal) {

        if (principal == null) {
            log.warn("Unauthenticated user tried to delete comment");
            return;
        }

        UUID userId = UUID.fromString(principal.getName());
        log.info("Deleting comment {} for course {} by user {}", commentId, courseId, userId);

        CommentWebSocketMessage message = CommentWebSocketMessage.deleted(
                commentId,
                courseId,
                CommentTargetType.COURSE,
                userId
        );

        String destination = "/topic/course/" + courseId + "/comments";
        messagingTemplate.convertAndSend(destination, message);
    }

    /**
     * Error handler
     */
    @MessageMapping("/course/error")
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
