package com.techhub.app.commonservice.websocket.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

/**
 * DTO cho error response qua WebSocket.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WebSocketErrorResponse {

    /**
     * Error code
     */
    private String code;

    /**
     * Error message
     */
    private String message;

    /**
     * Timestamp của error
     */
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSXXX")
    private OffsetDateTime timestamp;

    /**
     * Tạo error response
     */
    public static WebSocketErrorResponse of(String code, String message) {
        return WebSocketErrorResponse.builder()
                .code(code)
                .message(message)
                .timestamp(OffsetDateTime.now())
                .build();
    }

    /**
     * Unauthorized error
     */
    public static WebSocketErrorResponse unauthorized(String message) {
        return of("UNAUTHORIZED", message);
    }

    /**
     * Bad request error
     */
    public static WebSocketErrorResponse badRequest(String message) {
        return of("BAD_REQUEST", message);
    }

    /**
     * Not found error
     */
    public static WebSocketErrorResponse notFound(String message) {
        return of("NOT_FOUND", message);
    }

    /**
     * Forbidden error
     */
    public static WebSocketErrorResponse forbidden(String message) {
        return of("FORBIDDEN", message);
    }

    /**
     * Internal server error
     */
    public static WebSocketErrorResponse internalError(String message) {
        return of("INTERNAL_ERROR", message);
    }
}
