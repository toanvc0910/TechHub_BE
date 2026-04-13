package com.techhub.app.paymentservice.controller;

import com.techhub.app.commonservice.payload.GlobalResponse;
import com.techhub.app.paymentservice.dto.request.CreatePayoutRequestRequest;
import com.techhub.app.paymentservice.dto.request.MarkPaidPayoutRequest;
import com.techhub.app.paymentservice.dto.request.ReviewPayoutRequestRequest;
import com.techhub.app.paymentservice.dto.response.PayoutBalanceResponse;
import com.techhub.app.paymentservice.dto.response.PayoutBatchResponse;
import com.techhub.app.paymentservice.dto.response.PayoutRequestResponse;
import com.techhub.app.paymentservice.service.PayoutService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("api/v1/payouts")
@RequiredArgsConstructor
@Validated
public class PayoutController {

    private final PayoutService payoutService;

    @GetMapping("/balance")
    public ResponseEntity<GlobalResponse<PayoutBalanceResponse>> getBalance(
            @RequestHeader(value = "X-User-Roles", required = false) String userRoles,
            @RequestHeader(value = "X-User-Id", required = false) String userId,
            @RequestParam(value = "instructorId", required = false) UUID instructorId) {
        try {
            UUID targetInstructorId = resolveInstructorId(userRoles, userId, instructorId);
            PayoutBalanceResponse response = payoutService.getBalance(targetInstructorId);
            return ResponseEntity.ok(GlobalResponse.success("Payout balance", response));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(GlobalResponse.error(ex.getMessage(), HttpStatus.BAD_REQUEST.value()));
        }
    }

    @PostMapping("/requests")
    public ResponseEntity<GlobalResponse<PayoutRequestResponse>> createRequest(
            @RequestHeader(value = "X-User-Id", required = false) String userId,
            @Valid @RequestBody CreatePayoutRequestRequest request) {
        try {
            UUID requesterId = parseRequiredUserId(userId);
            PayoutRequestResponse response = payoutService.createRequest(requesterId, request);
            return ResponseEntity.ok(GlobalResponse.success("Payout request created", response));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(GlobalResponse.error(ex.getMessage(), HttpStatus.BAD_REQUEST.value()));
        }
    }

    @GetMapping("/requests")
    public ResponseEntity<GlobalResponse<List<PayoutRequestResponse>>> listRequests(
            @RequestHeader(value = "X-User-Roles", required = false) String userRoles,
            @RequestHeader(value = "X-User-Id", required = false) String userId) {
        try {
            boolean admin = hasAdminRole(userRoles);
            UUID requesterId = parseRequiredUserId(userId);
            List<PayoutRequestResponse> response = payoutService.listRequests(requesterId, admin);
            return ResponseEntity.ok(GlobalResponse.success("Payout requests", response));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(GlobalResponse.error(ex.getMessage(), HttpStatus.BAD_REQUEST.value()));
        }
    }

    @GetMapping("/requests/{requestId}")
    public ResponseEntity<GlobalResponse<PayoutRequestResponse>> getRequest(
            @RequestHeader(value = "X-User-Roles", required = false) String userRoles,
            @RequestHeader(value = "X-User-Id", required = false) String userId,
            @PathVariable UUID requestId) {
        try {
            boolean admin = hasAdminRole(userRoles);
            UUID requesterId = parseRequiredUserId(userId);
            PayoutRequestResponse response = payoutService.getRequest(requestId, requesterId, admin);
            return ResponseEntity.ok(GlobalResponse.success("Payout request detail", response));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(GlobalResponse.error(ex.getMessage(), HttpStatus.BAD_REQUEST.value()));
        }
    }

    @PutMapping("/requests/{requestId}/approve")
    public ResponseEntity<GlobalResponse<PayoutRequestResponse>> approveRequest(
            @RequestHeader(value = "X-User-Roles", required = false) String userRoles,
            @RequestHeader(value = "X-User-Id", required = false) String userId,
            @PathVariable UUID requestId,
            @RequestBody(required = false) ReviewPayoutRequestRequest request) {
        if (!hasAdminRole(userRoles)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(GlobalResponse.error("Admin role is required", HttpStatus.FORBIDDEN.value()));
        }

        try {
            UUID reviewerId = parseRequiredUserId(userId);
            ReviewPayoutRequestRequest body = request == null ? new ReviewPayoutRequestRequest() : request;
            PayoutRequestResponse response = payoutService.approveRequest(requestId, reviewerId, body);
            return ResponseEntity.ok(GlobalResponse.success("Payout request approved", response));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(GlobalResponse.error(ex.getMessage(), HttpStatus.BAD_REQUEST.value()));
        }
    }

    @PutMapping("/requests/{requestId}/reject")
    public ResponseEntity<GlobalResponse<PayoutRequestResponse>> rejectRequest(
            @RequestHeader(value = "X-User-Roles", required = false) String userRoles,
            @RequestHeader(value = "X-User-Id", required = false) String userId,
            @PathVariable UUID requestId,
            @RequestBody(required = false) ReviewPayoutRequestRequest request) {
        if (!hasAdminRole(userRoles)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(GlobalResponse.error("Admin role is required", HttpStatus.FORBIDDEN.value()));
        }

        try {
            UUID reviewerId = parseRequiredUserId(userId);
            ReviewPayoutRequestRequest body = request == null ? new ReviewPayoutRequestRequest() : request;
            PayoutRequestResponse response = payoutService.rejectRequest(requestId, reviewerId, body);
            return ResponseEntity.ok(GlobalResponse.success("Payout request rejected", response));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(GlobalResponse.error(ex.getMessage(), HttpStatus.BAD_REQUEST.value()));
        }
    }

    @PutMapping("/requests/{requestId}/mark-paid")
    public ResponseEntity<GlobalResponse<PayoutRequestResponse>> markPaid(
            @RequestHeader(value = "X-User-Roles", required = false) String userRoles,
            @RequestHeader(value = "X-User-Id", required = false) String userId,
            @PathVariable UUID requestId,
            @Valid @RequestBody MarkPaidPayoutRequest request) {
        if (!hasAdminRole(userRoles)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(GlobalResponse.error("Admin role is required", HttpStatus.FORBIDDEN.value()));
        }

        try {
            UUID markerId = parseRequiredUserId(userId);
            PayoutRequestResponse response = payoutService.markPaid(requestId, markerId, request);
            return ResponseEntity.ok(GlobalResponse.success("Payout marked as paid", response));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(GlobalResponse.error(ex.getMessage(), HttpStatus.BAD_REQUEST.value()));
        }
    }

    @GetMapping("/batches")
    public ResponseEntity<GlobalResponse<List<PayoutBatchResponse>>> listBatches(
            @RequestHeader(value = "X-User-Roles", required = false) String userRoles) {
        if (!hasAdminRole(userRoles)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(GlobalResponse.error("Admin role is required", HttpStatus.FORBIDDEN.value()));
        }
        List<PayoutBatchResponse> response = payoutService.listBatches();
        return ResponseEntity.ok(GlobalResponse.success("Payout batches", response));
    }

    @PostMapping("/batches/monthly")
    public ResponseEntity<GlobalResponse<PayoutBatchResponse>> createMonthlyBatch(
            @RequestHeader(value = "X-User-Roles", required = false) String userRoles,
            @RequestParam(value = "period", required = false) String period) {
        if (!hasAdminRole(userRoles)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(GlobalResponse.error("Admin role is required", HttpStatus.FORBIDDEN.value()));
        }

        try {
            YearMonth yearMonth = period == null || period.isBlank() ? null : YearMonth.parse(period);
            PayoutBatchResponse response = payoutService.createMonthlyBatch(yearMonth);
            return ResponseEntity.ok(GlobalResponse.success("Monthly payout batch created", response));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(GlobalResponse.error(ex.getMessage(), HttpStatus.BAD_REQUEST.value()));
        }
    }

    @PostMapping("/batches/manual")
    public ResponseEntity<GlobalResponse<PayoutBatchResponse>> createManualBatch(
            @RequestHeader(value = "X-User-Roles", required = false) String userRoles,
            @RequestParam(value = "name", required = false) String name,
            @RequestParam("fromDate") LocalDate fromDate,
            @RequestParam("toDate") LocalDate toDate) {
        if (!hasAdminRole(userRoles)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(GlobalResponse.error("Admin role is required", HttpStatus.FORBIDDEN.value()));
        }

        try {
            PayoutBatchResponse response = payoutService.createManualBatch(name, fromDate, toDate);
            return ResponseEntity.ok(GlobalResponse.success("Manual payout batch created", response));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(GlobalResponse.error(ex.getMessage(), HttpStatus.BAD_REQUEST.value()));
        }
    }

    private UUID resolveInstructorId(String rolesHeader, String userId, UUID instructorIdParam) {
        if (hasAdminRole(rolesHeader)) {
            if (instructorIdParam != null) {
                return instructorIdParam;
            }
            return parseRequiredUserId(userId);
        }
        return parseRequiredUserId(userId);
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
