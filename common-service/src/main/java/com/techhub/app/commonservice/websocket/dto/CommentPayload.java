package com.techhub.app.commonservice.websocket.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;
import java.util.UUID;

/**
 * DTO cho request payload khi client gửi comment qua WebSocket.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CommentPayload {

    /**
     * Nội dung comment (required)
     */
    @NotBlank(message = "Content is required")
    @Size(min = 1, max = 2000, message = "Content must be between 1 and 2000 characters")
    private String content;

    /**
     * ID của parent comment (optional - nếu là reply)
     */
    private UUID parentId;
}
