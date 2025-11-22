package com.techhub.app.paymentservice.controller;

import com.techhub.app.paymentservice.config.RestResponseObject;
import com.techhub.app.paymentservice.config.VNPAYConfig;
import com.techhub.app.paymentservice.dto.response.VNPayPaymentDTO;
import com.techhub.app.paymentservice.service.VNPayPaymentService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Comparator;
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
    public RestResponseObject<VNPayPaymentDTO.VNPayResponse> pay(
            HttpServletRequest request,
            @RequestParam(required = false) String userId) {

//        log.info("Creating VNPay payment with amount: {}, userId: {}",
//                request.getParameter("amount"), userId);

        // Validate userId is provided
        if (userId == null || userId.isEmpty()) {
            log.error("VNPay payment requires userId parameter");
            throw new IllegalArgumentException("userId parameter is required for VNPay payment");
        }

        // Validate userId format
        try {
            UUID.fromString(userId);
        } catch (IllegalArgumentException e) {
            log.error("Invalid userId format: {}", userId);
            throw new IllegalArgumentException("Invalid userId format: " + userId);
        }

        // Add userId to request attributes for service to access
        request.setAttribute("userId", userId);

        return new RestResponseObject<>(HttpStatus.OK, "Success", paymentService.createVnPayPayment(request));
    }

    @GetMapping("/vn-pay-callback")
    public void payCallbackHandler(HttpServletRequest request, HttpServletResponse response) throws IOException {
        // Lấy tất cả tham số từ yêu cầu
        Map<String, String> params = request.getParameterMap().entrySet().stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        entry -> entry.getValue()[0]
                ));

        // Lấy các tham số quan trọng
        String vnp_SecureHash = params.get("vnp_SecureHash");
        String vnp_TransactionStatus = params.get("vnp_TransactionStatus");
        String vnp_TxnRef = params.get("vnp_TxnRef");
        String vnp_Amount = params.get("vnp_Amount");

        // Xác minh chữ ký
        boolean isValid = verifySecureHash(params, vnp_SecureHash, vnpayConfig.getSecretKey());

        // Lưu lịch sử giao dịch vào database
        try {
            paymentService.handlePaymentCallback(params, isValid, vnp_TransactionStatus);
        } catch (Exception e) {
            log.error("Error saving payment history: {}", e.getMessage(), e);
        }

        // URL trang kết quả trên frontend - sử dụng VNPay frontend URL
        String frontendResultUrl = vnpayConfig.getFrontendVnpayReturnUrl();

        // Tạo URL chuyển hướng với các tham số
        String status = isValid && "00".equals(vnp_TransactionStatus) ? "success" : "failed";
        String redirectUrl = frontendResultUrl + "?status=" + URLEncoder.encode(status, StandardCharsets.UTF_8) +
                "&paymentMethod=" + URLEncoder.encode("VNPay", StandardCharsets.UTF_8) +
                "&txnRef=" + URLEncoder.encode(vnp_TxnRef != null ? vnp_TxnRef : "N/A", StandardCharsets.UTF_8) +
                "&amount=" + URLEncoder.encode(vnp_Amount != null ? vnp_Amount : "0", StandardCharsets.UTF_8);

        // Ghi log để gỡ lỗi
//        log.info("VNPay Callback - Valid: {}, Status: {}, TxnRef: {}", isValid, status, vnp_TxnRef);
//        log.info("Redirecting to frontend: {}", redirectUrl);

        // Chuyển hướng đến trang frontend
        response.sendRedirect(redirectUrl);
    }

    private boolean verifySecureHash(Map<String, String> params, String secureHash, String secretKey) {
        // Loại bỏ các tham số không cần thiết và sắp xếp theo khóa
        Map<String, String> sortedParams = params.entrySet().stream()
                .filter(entry -> !entry.getKey().equals("vnp_SecureHash") && !entry.getKey().equals("vnp_SecureHashType"))
                .sorted(Comparator.comparing(Map.Entry::getKey)) // Sửa lỗi bằng Comparator.comparing
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        Map.Entry::getValue,
                        (e1, e2) -> e1,
                        java.util.LinkedHashMap::new
                ));

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
        } catch (Exception e) {
            System.err.println("Error verifying secure hash: " + e.getMessage());
            return false;
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
