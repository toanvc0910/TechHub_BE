package com.techhub.app.commonservice.payload;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.techhub.app.commonservice.exception.ErrorCode;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class GlobalResponse<T> {

    private boolean success;
    private String status;
    private String message;
    private T data;
    private LocalDateTime timestamp;
    private String path;
    private Integer code;

    public static <T> GlobalResponse<T> success(T data) {
        return GlobalResponse.<T>builder()
                .success(true)
                .status("SUCCESS")
                .message("Operation completed successfully")
                .data(data)
                .timestamp(LocalDateTime.now())
                .code(200)
                .build();
    }

    public static <T> GlobalResponse<T> success(String message, T data) {
        return GlobalResponse.<T>builder()
                .success(true)
                .status("SUCCESS")
                .message(message)
                .data(data)
                .timestamp(LocalDateTime.now())
                .code(200)
                .build();
    }

    public static <T> GlobalResponse<T> error(String message) {
        return GlobalResponse.<T>builder()
                .success(false)
                .status("ERROR")
                .message(message)
                .timestamp(LocalDateTime.now())
                .code(400)
                .build();
    }

    public static <T> GlobalResponse<T> error(String message, Integer code) {
        return GlobalResponse.<T>builder()
                .success(false)
                .status("ERROR")
                .message(message)
                .timestamp(LocalDateTime.now())
                .code(code)
                .build();
    }

    public static <T> GlobalResponse<T> error(ErrorCode errorCode, String message) {
        return GlobalResponse.<T>builder()
                .success(false)
                .status(errorCode.getCode())
                .message(message)
                .timestamp(LocalDateTime.now())
                .code(errorCode.getHttpStatus().value())
                .build();
    }

    public GlobalResponse<T> withPath(String path) {
        this.path = path;
        return this;
    }

    public GlobalResponse<T> withStatus(String status) {
        this.status = status;
        return this;
    }
}
