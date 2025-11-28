package com.techhub.app.commonservice.websocket.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * DTO cho WebSocket message của comment.
 * Được gửi từ server đến clients khi có event comment.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CommentWebSocketMessage {

    /**
     * Loại event (CREATED, UPDATED, DELETED, REPLIED)
     */
    private CommentEventType eventType;

    /**
     * ID của comment
     */
    private UUID commentId;

    /**
     * ID của target (blogId, courseId, lessonId)
     */
    private UUID targetId;

    /**
     * Loại target (BLOG, COURSE, LESSON)
     */
    private CommentTargetType targetType;

    /**
     * ID của parent comment (nếu là reply)
     */
    private UUID parentId;

    /**
     * ID của user tạo/sửa comment
     */
    private UUID userId;

    /**
     * Username hiển thị
     */
    private String username;

    /**
     * Avatar URL của user
     */
    private String avatarUrl;

    /**
     * Nội dung comment
     */
    private String content;

    /**
     * Thời gian tạo comment
     */
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSXXX")
    private OffsetDateTime createdAt;

    /**
     * Thời gian event xảy ra
     */
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSXXX")
    private OffsetDateTime timestamp;

    /**
     * Tạo message cho event CREATED
     */
    public static CommentWebSocketMessage created(UUID commentId, UUID targetId, CommentTargetType targetType,
                                                   UUID parentId, UUID userId, String username, 
                                                   String avatarUrl, String content, OffsetDateTime createdAt) {
        return CommentWebSocketMessage.builder()
                .eventType(CommentEventType.CREATED)
                .commentId(commentId)
                .targetId(targetId)
                .targetType(targetType)
                .parentId(parentId)
                .userId(userId)
                .username(username)
                .avatarUrl(avatarUrl)
                .content(content)
                .createdAt(createdAt)
                .timestamp(OffsetDateTime.now())
                .build();
    }

    /**
     * Tạo message cho event REPLIED
     */
    public static CommentWebSocketMessage replied(UUID commentId, UUID targetId, CommentTargetType targetType,
                                                   UUID parentId, UUID userId, String username,
                                                   String avatarUrl, String content, OffsetDateTime createdAt) {
        return CommentWebSocketMessage.builder()
                .eventType(CommentEventType.REPLIED)
                .commentId(commentId)
                .targetId(targetId)
                .targetType(targetType)
                .parentId(parentId)
                .userId(userId)
                .username(username)
                .avatarUrl(avatarUrl)
                .content(content)
                .createdAt(createdAt)
                .timestamp(OffsetDateTime.now())
                .build();
    }

    /**
     * Tạo message cho event UPDATED
     */
    public static CommentWebSocketMessage updated(UUID commentId, UUID targetId, CommentTargetType targetType,
                                                   UUID userId, String content) {
        return CommentWebSocketMessage.builder()
                .eventType(CommentEventType.UPDATED)
                .commentId(commentId)
                .targetId(targetId)
                .targetType(targetType)
                .userId(userId)
                .content(content)
                .timestamp(OffsetDateTime.now())
                .build();
    }

    /**
     * Tạo message cho event DELETED
     */
    public static CommentWebSocketMessage deleted(UUID commentId, UUID targetId, CommentTargetType targetType,
                                                   UUID userId) {
        return CommentWebSocketMessage.builder()
                .eventType(CommentEventType.DELETED)
                .commentId(commentId)
                .targetId(targetId)
                .targetType(targetType)
                .userId(userId)
                .timestamp(OffsetDateTime.now())
                .build();
    }
}
