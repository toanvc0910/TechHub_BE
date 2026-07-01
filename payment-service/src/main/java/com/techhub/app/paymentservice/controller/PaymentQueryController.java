package com.techhub.app.paymentservice.controller;

import com.techhub.app.commonservice.payload.GlobalResponse;
import com.techhub.app.paymentservice.dto.response.PaymentHistoryItemResponse;
import com.techhub.app.paymentservice.service.TransactionService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
public class PaymentQueryController {

    private final TransactionService transactionService;

    @GetMapping("/history")
    public ResponseEntity<GlobalResponse<Page<PaymentHistoryItemResponse>>> getPaymentHistory(
            @RequestHeader(value = "X-User-Roles", required = false) String userRoles,
            @RequestHeader(value = "X-User-Id", required = false) String userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        UUID requesterId = parseRequiredUserId(userId);
        boolean admin = hasAdminRole(userRoles);
        Page<PaymentHistoryItemResponse> history = transactionService.getPaymentHistory(requesterId, admin, page, size);
        return ResponseEntity.ok(GlobalResponse.success(history));
    }

    @GetMapping("/{paymentId:[0-9a-fA-F-]{36}}")
    public ResponseEntity<GlobalResponse<PaymentHistoryItemResponse>> getPaymentById(@PathVariable UUID paymentId) {
        PaymentHistoryItemResponse payment = transactionService.getPaymentById(paymentId);
        return ResponseEntity.ok(GlobalResponse.success(payment));
    }

    private UUID parseRequiredUserId(String userId) {
        if (userId == null || userId.isBlank()) {
            throw new IllegalArgumentException("Missing X-User-Id header");
        }
        try {
            return UUID.fromString(userId);
        } catch (Exception ex) {
            throw new IllegalArgumentException("Invalid X-User-Id header");
        }
    }

    private boolean hasAdminRole(String rolesHeader) {
        if (rolesHeader == null || rolesHeader.isBlank()) {
            return false;
        }
        return rolesHeader.toUpperCase().contains("ADMIN");
    }
}
