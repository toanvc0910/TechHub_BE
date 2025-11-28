package com.techhub.app.commonservice.websocket.dto;

/**
 * Enum định nghĩa các loại event cho comment WebSocket.
 */
public enum CommentEventType {
    /**
     * Comment mới được tạo
     */
    CREATED,

    /**
     * Comment được cập nhật
     */
    UPDATED,

    /**
     * Comment bị xóa
     */
    DELETED,

    /**
     * Có reply mới cho comment
     */
    REPLIED
}
