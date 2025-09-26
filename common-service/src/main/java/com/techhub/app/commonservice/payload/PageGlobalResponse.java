package com.techhub.app.commonservice.payload;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PageGlobalResponse<T> {

    private boolean success;
    private String status;
    private String message;
    private List<T> data;
    private PaginationInfo pagination;
    private LocalDateTime timestamp;
    private String path;
    private Integer code;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PaginationInfo {
        private int page;
        private int size;
        private long totalElements;
        private int totalPages;
        private boolean first;
        private boolean last;
        private boolean hasNext;
        private boolean hasPrevious;
    }

    public static <T> PageGlobalResponse<T> success(List<T> data, PaginationInfo pagination) {
        return PageGlobalResponse.<T>builder()
                .success(true)
                .status("SUCCESS")
                .message("Operation completed successfully")
                .data(data)
                .pagination(pagination)
                .timestamp(LocalDateTime.now())
                .code(200)
                .build();
    }

    public static <T> PageGlobalResponse<T> success(String message, List<T> data, PaginationInfo pagination) {
        return PageGlobalResponse.<T>builder()
                .success(true)
                .status("SUCCESS")
                .message(message)
                .data(data)
                .pagination(pagination)
                .timestamp(LocalDateTime.now())
                .code(200)
                .build();
    }

    public static <T> PageGlobalResponse<T> error(String message) {
        return PageGlobalResponse.<T>builder()
                .success(false)
                .status("ERROR")
                .message(message)
                .timestamp(LocalDateTime.now())
                .code(400)
                .build();
    }

    public PageGlobalResponse<T> withPath(String path) {
        this.path = path;
        return this;
    }
}
