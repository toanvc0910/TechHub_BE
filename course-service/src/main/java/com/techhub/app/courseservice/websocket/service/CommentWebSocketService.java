package com.techhub.app.courseservice.websocket.service;

import com.techhub.app.commonservice.websocket.dto.CommentTargetType;
import com.techhub.app.commonservice.websocket.dto.CommentWebSocketMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * Service để gửi WebSocket messages cho comments trong Course Service.
 * Được sử dụng bởi các REST controllers hoặc services khác 
 * khi cần broadcast comment updates cho Course hoặc Lesson.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CommentWebSocketService {

    private final SimpMessagingTemplate messagingTemplate;

    /**
     * Broadcast khi có comment mới được tạo.
     */
    public void broadcastCommentCreated(CommentWebSocketMessage message) {
        String destination = getDestination(message.getTargetType(), message.getTargetId());
        messagingTemplate.convertAndSend(destination, message);
        log.info("Broadcasted CREATED comment {} to {}", message.getCommentId(), destination);
    }

    /**
     * Broadcast khi comment được cập nhật.
     */
    public void broadcastCommentUpdated(CommentWebSocketMessage message) {
        String destination = getDestination(message.getTargetType(), message.getTargetId());
        messagingTemplate.convertAndSend(destination, message);
        log.info("Broadcasted UPDATED comment {} to {}", message.getCommentId(), destination);
    }

    /**
     * Broadcast khi comment bị xóa.
     */
    public void broadcastCommentDeleted(CommentWebSocketMessage message) {
        String destination = getDestination(message.getTargetType(), message.getTargetId());
        messagingTemplate.convertAndSend(destination, message);
        log.info("Broadcasted DELETED comment {} to {}", message.getCommentId(), destination);
    }

    /**
     * Gửi notification đến user cụ thể (private message).
     * Dùng cho reply notifications.
     */
    public void sendToUser(UUID userId, CommentWebSocketMessage message) {
        String destination = "/user/" + userId + "/queue/comments";
        messagingTemplate.convertAndSend(destination, message);
        log.info("Sent notification to user {} for comment {}", userId, message.getCommentId());
    }

    /**
     * Build destination path dựa trên target type và id.
     */
    private String getDestination(CommentTargetType targetType, UUID targetId) {
        switch (targetType) {
            case COURSE:
                return "/topic/course/" + targetId + "/comments";
            case LESSON:
                return "/topic/lesson/" + targetId + "/comments";
            default:
                throw new IllegalArgumentException("Unsupported target type: " + targetType);
        }
    }
}
