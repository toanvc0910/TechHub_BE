package com.techhub.app.proxyclient.exception;

import com.techhub.app.commonservice.exception.ServiceException;
import com.techhub.app.commonservice.payload.GlobalResponse;
import feign.FeignException;
import feign.RetryableException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import javax.servlet.http.HttpServletRequest;

@RestControllerAdvice
@Slf4j
public class ProxyExceptionHandler {

    @ExceptionHandler(ServiceException.class)
    public ResponseEntity<GlobalResponse<Object>> handleServiceException(ServiceException exception,
            HttpServletRequest request) {
        log.warn("Business error at {} -> status: {}, code: {}, message: {}",
                request.getRequestURI(),
                exception.getStatus().value(),
                exception.getCode(),
                exception.getMessage());

        GlobalResponse<Object> body = GlobalResponse.error(exception.getMessage(), exception.getStatus().value())
                .withStatus(exception.getCode())
                .withPath(request.getRequestURI());

        return ResponseEntity.status(exception.getStatus()).body(body);
    }

    @ExceptionHandler(FeignException.class)
    public ResponseEntity<String> handleFeignException(FeignException exception, HttpServletRequest request) {
        int upstreamStatus = exception.status();
        HttpStatus status = upstreamStatus > 0
                ? HttpStatus.resolve(upstreamStatus)
                : null;
        if (status == null) {
            status = HttpStatus.BAD_GATEWAY;
        }

        String upstreamBody = exception.contentUTF8();
        log.warn("Upstream error at {} -> status: {}, message: {}", request.getRequestURI(), status.value(),
                exception.getMessage());

        if (upstreamBody != null && !upstreamBody.isBlank()) {
            return ResponseEntity.status(status)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(upstreamBody);
        }

        String fallbackBody = String.format(
                "{\"success\":false,\"status\":\"UPSTREAM_ERROR\",\"code\":%d,\"message\":\"Upstream service error\",\"path\":\"%s\"}",
                status.value(), request.getRequestURI());

        return ResponseEntity.status(status)
                .contentType(MediaType.APPLICATION_JSON)
                .body(fallbackBody);
    }

    @ExceptionHandler(RetryableException.class)
    public ResponseEntity<String> handleRetryableException(RetryableException exception, HttpServletRequest request) {
        log.error("Upstream unavailable at {}: {}", request.getRequestURI(), exception.getMessage());

        String body = String.format(
                "{\"success\":false,\"status\":\"SERVICE_UNAVAILABLE\",\"code\":503,\"message\":\"Upstream service is unavailable\",\"path\":\"%s\"}",
                request.getRequestURI());

        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .contentType(MediaType.APPLICATION_JSON)
                .body(body);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<GlobalResponse<Object>> handleIllegalArgumentException(IllegalArgumentException exception,
            HttpServletRequest request) {
        GlobalResponse<Object> body = GlobalResponse.error(exception.getMessage(), HttpStatus.BAD_REQUEST.value())
                .withStatus("BAD_REQUEST")
                .withPath(request.getRequestURI());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<GlobalResponse<Object>> handleUnhandledException(Exception exception,
            HttpServletRequest request) {
        log.error("Unhandled proxy error at {}: {}", request.getRequestURI(), exception.getMessage(), exception);
        GlobalResponse<Object> body = GlobalResponse
                .error("Internal server error", HttpStatus.INTERNAL_SERVER_ERROR.value())
                .withStatus("INTERNAL_SERVER_ERROR")
                .withPath(request.getRequestURI());
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(body);
    }
}
