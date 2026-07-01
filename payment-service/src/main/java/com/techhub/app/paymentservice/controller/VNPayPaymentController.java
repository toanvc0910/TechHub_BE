package com.techhub.app.paymentservice.controller;

import com.techhub.app.commonservice.payload.GlobalResponse;
import com.techhub.app.paymentservice.config.VNPAYConfig;
import com.techhub.app.paymentservice.dto.response.VNPayPaymentDTO;
import com.techhub.app.paymentservice.service.VNPayPaymentService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("api/v1/payment")
public class VNPayPaymentController {
    private final VNPayPaymentService paymentService;
    private final VNPAYConfig vnpayConfig;

    @Autowired
    public VNPayPaymentController(VNPayPaymentService paymentService, VNPAYConfig vnpayConfig) {
        this.paymentService = paymentService;
        this.vnpayConfig = vnpayConfig;
    }

    @GetMapping("/vn-pay")
    public ResponseEntity<GlobalResponse<VNPayPaymentDTO.VNPayResponse>> pay(
            HttpServletRequest request,
            @RequestParam(required = false) String userId,
            @RequestParam(required = false) String courseId) {

        // log.info("Creating VNPay payment with amount: {}, userId: {}",
        // request.getParameter("amount"), userId);

        // Validate userId is provided
        if (userId == null || userId.isEmpty()) {
            log.error("VNPay payment requires userId parameter");
            throw new IllegalArgumentException("userId parameter is required for VNPay payment");
        }

        // Validate courseId is provided
        if (courseId == null || courseId.isEmpty()) {
            log.error("VNPay payment requires courseId parameter");
            throw new IllegalArgumentException("courseId parameter is required for VNPay payment");
        }

        // Validate userId format
        try {
            UUID.fromString(userId);
        } catch (IllegalArgumentException e) {
            log.error("Invalid userId format: {}", userId);
            throw new IllegalArgumentException("Invalid userId format: " + userId);
        }

        // Validate courseId format
        try {
            UUID.fromString(courseId);
        } catch (IllegalArgumentException e) {
            log.error("Invalid courseId format: {}", courseId);
            throw new IllegalArgumentException("Invalid courseId format: " + courseId);
        }

        // Add userId and courseId to request attributes for service to access
        request.setAttribute("userId", userId);
        request.setAttribute("courseId", courseId);

        return ResponseEntity.ok(GlobalResponse.success(paymentService.createVnPayPayment(request)));
    }

    @GetMapping("/vn-pay-callback")
    public void payCallbackHandler(HttpServletRequest request, HttpServletResponse response) throws IOException {
        response.sendRedirect(buildFrontendRedirectUrl(buildCallbackResult(request)));
    }

    @GetMapping("/vn-pay-callback-result")
    public ResponseEntity<Map<String, String>> payCallbackResult(HttpServletRequest request) {
        return ResponseEntity.ok(buildCallbackResult(request));
    }

    private Map<String, String> buildCallbackResult(HttpServletRequest request) {
        Map<String, String> params = request.getParameterMap().entrySet().stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        entry -> entry.getValue()[0]));

        String vnp_SecureHash = params.get("vnp_SecureHash");
        String vnp_TransactionStatus = params.get("vnp_TransactionStatus");
        String vnp_TxnRef = params.get("vnp_TxnRef");
        String vnp_Amount = params.get("vnp_Amount");

        boolean isValid = verifySecureHash(params, vnp_SecureHash, vnpayConfig.getSecretKey());

        try {
            paymentService.handlePaymentCallback(params, isValid, vnp_TransactionStatus);
        } catch (RuntimeException e) {
            log.error("Error saving payment history: {}", e.getMessage(), e);
        }

        String status = isValid && "00".equals(vnp_TransactionStatus) ? "success" : "failed";
        Map<String, String> result = new LinkedHashMap<>();
        result.put("status", status);
        result.put("paymentMethod", "VNPay");
        result.put("txnRef", vnp_TxnRef != null ? vnp_TxnRef : "N/A");
        result.put("amount", normalizeVnpAmount(vnp_Amount));
        return result;
    }

    private String buildFrontendRedirectUrl(Map<String, String> params) {
        StringBuilder redirectUrl = new StringBuilder(vnpayConfig.getFrontendVnpayReturnUrl());
        redirectUrl.append("?");

        boolean first = true;
        for (Map.Entry<String, String> entry : params.entrySet()) {
            if (!first) {
                redirectUrl.append("&");
            }
            redirectUrl.append(URLEncoder.encode(entry.getKey(), StandardCharsets.UTF_8));
            redirectUrl.append("=");
            redirectUrl.append(URLEncoder.encode(entry.getValue(), StandardCharsets.UTF_8));
            first = false;
        }

        return redirectUrl.toString();
    }

    private boolean verifySecureHash(Map<String, String> params, String secureHash, String secretKey) {
        // Loại bỏ các tham số không cần thiết và sắp xếp theo khóa
        Map<String, String> sortedParams = params.entrySet().stream()
                .filter(entry -> !entry.getKey().equals("vnp_SecureHash")
                        && !entry.getKey().equals("vnp_SecureHashType"))
                .sorted(Comparator.comparing(Map.Entry::getKey)) // Sửa lỗi bằng Comparator.comparing
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        Map.Entry::getValue,
                        (e1, e2) -> e1,
                        java.util.LinkedHashMap::new));

        // Tạo chuỗi ký tự để hash
        String signData = sortedParams.entrySet().stream()
                .map(entry -> entry.getKey() + "=" + URLEncoder.encode(entry.getValue(), StandardCharsets.UTF_8))
                .collect(Collectors.joining("&"));

        try {
            Mac mac = Mac.getInstance("HmacSHA512");
            SecretKeySpec secretKeySpec = new SecretKeySpec(secretKey.getBytes(StandardCharsets.UTF_8), "HmacSHA512");
            mac.init(secretKeySpec);
            byte[] hashBytes = mac.doFinal(signData.getBytes(StandardCharsets.UTF_8));
            String calculatedHash = bytesToHex(hashBytes);
            return calculatedHash.equalsIgnoreCase(secureHash);
        } catch (GeneralSecurityException e) {
            System.err.println("Error verifying secure hash: " + e.getMessage());
            return false;
        }
    }

    private String normalizeVnpAmount(String vnpAmount) {
        if (vnpAmount == null || vnpAmount.isBlank()) {
            return "0";
        }
        try {
            return new BigDecimal(vnpAmount)
                    .movePointLeft(2)
                    .setScale(0, RoundingMode.HALF_UP)
                    .toPlainString();
        } catch (NumberFormatException ex) {
            return "0";
        }
    }

    private String bytesToHex(byte[] bytes) {
        StringBuilder result = new StringBuilder();
        for (byte b : bytes) {
            result.append(String.format("%02x", b));
        }
        return result.toString();
    }
}
