package com.techhub.app.commonservice.exception;

import com.techhub.app.commonservice.payload.GlobalResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import javax.servlet.http.HttpServletRequest;
import javax.validation.ConstraintViolationException;
import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
@Slf4j
public class ApiExceptionHandler {

    @ExceptionHandler(ServiceException.class)
    public ResponseEntity<GlobalResponse<Void>> handleServiceException(ServiceException exception,
                                                                       HttpServletRequest request) {
        log.warn("Service exception: {}", exception.getMessage());
        GlobalResponse<Void> response = GlobalResponse.<Void>error(exception.getErrorCode(), exception.getMessage())
                .withPath(request.getRequestURI());
        return ResponseEntity.status(exception.getStatus()).body(response);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<GlobalResponse<Map<String, String>>> handleMethodArgumentNotValid(MethodArgumentNotValidException exception,
                                                                                            HttpServletRequest request) {
        Map<String, String> errors = new HashMap<>();
        exception.getBindingResult().getAllErrors().forEach(error -> {
            String fieldName = error instanceof FieldError ? ((FieldError) error).getField() : error.getObjectName();
            errors.put(fieldName, error.getDefaultMessage());
        });

        GlobalResponse<Map<String, String>> response =
                GlobalResponse.<Map<String, String>>error(ErrorCode.VALIDATION_ERROR, "Validation failed")
                        .withPath(request.getRequestURI());
        response.setData(errors);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<GlobalResponse<Map<String, String>>> handleConstraintViolation(ConstraintViolationException exception,
                                                                                          HttpServletRequest request) {
        Map<String, String> errors = new HashMap<>();
        exception.getConstraintViolations().forEach(violation ->
                errors.put(violation.getPropertyPath().toString(), violation.getMessage())
        );

        GlobalResponse<Map<String, String>> response =
                GlobalResponse.<Map<String, String>>error(ErrorCode.VALIDATION_ERROR, "Constraint violation")
                        .withPath(request.getRequestURI());
        response.setData(errors);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<GlobalResponse<Void>> handleHttpMessageNotReadable(HttpMessageNotReadableException exception,
                                                                             HttpServletRequest request) {
        log.warn("Invalid payload: {}", exception.getMessage());
        GlobalResponse<Void> response =
                GlobalResponse.<Void>error(ErrorCode.BAD_REQUEST, "Request body is invalid or malformed")
                        .withPath(request.getRequestURI());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<GlobalResponse<Void>> handleDataIntegrityViolation(DataIntegrityViolationException exception,
                                                                             HttpServletRequest request) {
        log.error("Data integrity violation", exception);
        GlobalResponse<Void> response =
                GlobalResponse.<Void>error(ErrorCode.CONFLICT, "Request violates data constraints")
                        .withPath(request.getRequestURI());
        return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<GlobalResponse<Void>> handleIllegalArgument(IllegalArgumentException exception,
                                                                      HttpServletRequest request) {
        log.warn("Illegal argument: {}", exception.getMessage());
        GlobalResponse<Void> response =
                GlobalResponse.<Void>error(ErrorCode.BAD_REQUEST, exception.getMessage())
                        .withPath(request.getRequestURI());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<GlobalResponse<Void>> handleGenericException(Exception exception,
                                                                       HttpServletRequest request) {
        log.error("Unhandled exception", exception);
        GlobalResponse<Void> response =
                GlobalResponse.<Void>error(ErrorCode.UNKNOWN_ERROR, exception.getMessage())
                        .withPath(request.getRequestURI());
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }
}
