package com.techhub.app.paymentservice.service;

import com.techhub.app.paymentservice.config.VNPAYConfig;
import com.techhub.app.paymentservice.dto.response.VNPayPaymentDTO;
import com.techhub.app.paymentservice.entity.Payment;
import com.techhub.app.paymentservice.entity.Transaction;
import com.techhub.app.paymentservice.entity.enums.PaymentMethod;
import com.techhub.app.paymentservice.entity.enums.PaymentStatus;
import com.techhub.app.paymentservice.entity.enums.TransactionStatus;
import com.techhub.app.paymentservice.repository.PaymentRepository;
import com.techhub.app.paymentservice.repository.TransactionRepository;
import com.techhub.app.paymentservice.util.VNPayUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.servlet.http.HttpServletRequest;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
@Slf4j
public class VNPayPaymentService {

    private final VNPAYConfig vnPayConfig;
    private final TransactionRepository transactionRepository;
    private final PaymentRepository paymentRepository;

    public VNPayPaymentService(VNPAYConfig vnPayConfig,
                               TransactionRepository transactionRepository,
                               PaymentRepository paymentRepository) {
        this.vnPayConfig = vnPayConfig;
        this.transactionRepository = transactionRepository;
        this.paymentRepository = paymentRepository;
    }

    @Transactional
    public VNPayPaymentDTO.VNPayResponse createVnPayPayment(HttpServletRequest request) {
        long amount = Integer.parseInt(request.getParameter("amount")) * 100L;
        String bankCode = request.getParameter("bankCode");

        // Lấy userId từ request parameter hoặc attribute
        String userIdStr = request.getParameter("userId");
        if (userIdStr == null || userIdStr.isEmpty()) {
            userIdStr = (String) request.getAttribute("userId");
        }

        // Tạo transaction pending trước khi chuyển hướng đến VNPay
        UUID userId = null;
        if (userIdStr != null && !userIdStr.isEmpty()) {
            try {
                userId = UUID.fromString(userIdStr);
                log.info("Processing payment for user: {}", userId);
            } catch (IllegalArgumentException e) {
                log.warn("Invalid userId format: {}, using guest user", userIdStr);
            }
        }

        // Nếu không có userId, sử dụng một UUID mặc định cho guest user
        if (userId == null) {
            userId = UUID.fromString("00000000-0000-0000-0000-000000000000");
            log.warn("No userId provided, using guest user ID for anonymous transaction");
        }

        Transaction transaction = Transaction.builder()
                .userId(userId)
                .amount(BigDecimal.valueOf(amount / 100.0))
                .status(TransactionStatus.PENDING)
                .isActive("Y")
                .build();
        transaction = transactionRepository.save(transaction);

        Map<String, String> vnpParamsMap = vnPayConfig.getVNPayConfig();
        vnpParamsMap.put("vnp_Amount", String.valueOf(amount));
        // Sử dụng transaction ID làm vnp_TxnRef để tracking
        vnpParamsMap.put("vnp_TxnRef", transaction.getId().toString());

        if (bankCode != null && !bankCode.isEmpty()) {
            vnpParamsMap.put("vnp_BankCode", bankCode);
        }
        vnpParamsMap.put("vnp_IpAddr", VNPayUtil.getIpAddress(request));

        //build query url
        String queryUrl = VNPayUtil.getPaymentURL(vnpParamsMap, true);
        String hashData = VNPayUtil.getPaymentURL(vnpParamsMap, false);
        String vnpSecureHash = VNPayUtil.hmacSHA512(vnPayConfig.getSecretKey(), hashData);
        queryUrl += "&vnp_SecureHash=" + vnpSecureHash;
        String paymentUrl = vnPayConfig.getVnp_PayUrl() + "?" + queryUrl;

        log.info("Created pending transaction with ID: {} for user: {}, amount: {}",
                transaction.getId(), userId, amount / 100.0);

        return VNPayPaymentDTO.VNPayResponse.builder()
                .code("ok")
                .message("success")
                .paymentUrl(paymentUrl).build();
    }

    @Transactional
    public void handlePaymentCallback(Map<String, String> params, boolean isValid, String vnp_TransactionStatus) {
        String vnp_TxnRef = params.get("vnp_TxnRef");
        String vnp_Amount = params.get("vnp_Amount");
        String vnp_BankCode = params.get("vnp_BankCode");
        String vnp_CardType = params.get("vnp_CardType");
        String vnp_ResponseCode = params.get("vnp_ResponseCode");

        log.info("Processing payment callback - TxnRef: {}, Status: {}, ResponseCode: {}, IsValid: {}",
                vnp_TxnRef, vnp_TransactionStatus, vnp_ResponseCode, isValid);

        try {
            UUID transactionId = UUID.fromString(vnp_TxnRef);
            Transaction transaction = transactionRepository.findById(transactionId)
                    .orElseThrow(() -> new RuntimeException("Transaction not found: " + transactionId));

            log.info("Found transaction: {} with current status: {}", transactionId, transaction.getStatus());

            // Xác định trạng thái thanh toán
            PaymentStatus paymentStatus;
            TransactionStatus transactionStatus;

            if (isValid && "00".equals(vnp_TransactionStatus)) {
                paymentStatus = PaymentStatus.SUCCESS;
                transactionStatus = TransactionStatus.COMPLETED;
                log.info("Payment successful for transaction: {}", transactionId);
            } else {
                paymentStatus = PaymentStatus.FAILED;
                transactionStatus = TransactionStatus.PENDING; // Keep as PENDING if failed
                log.warn("Payment failed for transaction: {}. Response code: {}", transactionId, vnp_ResponseCode);
            }

            // Cập nhật transaction status
            transaction.setStatus(transactionStatus);
            transaction.setUpdated(java.time.ZonedDateTime.now());
            Transaction savedTransaction = transactionRepository.saveAndFlush(transaction);

            log.info("Updated transaction: {} to status: {} (saved status: {})",
                    transactionId, transactionStatus, savedTransaction.getStatus());

            // Lưu payment record với gateway response
            Map<String, Object> gatewayResponse = new HashMap<>();
            gatewayResponse.put("vnp_Amount", vnp_Amount);
            gatewayResponse.put("vnp_BankCode", vnp_BankCode);
            gatewayResponse.put("vnp_CardType", vnp_CardType);
            gatewayResponse.put("vnp_ResponseCode", vnp_ResponseCode);
            gatewayResponse.put("vnp_TransactionStatus", vnp_TransactionStatus);
            gatewayResponse.put("vnp_TxnRef", vnp_TxnRef);

            Payment payment = Payment.builder()
                    .transaction(savedTransaction)
                    .method(PaymentMethod.VNPAY)
                    .status(paymentStatus)
                    .gatewayResponse(gatewayResponse)
                    .isActive("Y")
                    .build();

            Payment savedPayment = paymentRepository.saveAndFlush(payment);
            log.info("Saved payment record: {} for transaction: {} with status: {}",
                    savedPayment.getId(), transactionId, paymentStatus);

        } catch (Exception e) {
            log.error("Error handling payment callback for txnRef: {}", vnp_TxnRef, e);
            throw new RuntimeException("Failed to process payment callback", e);
        }
    }
}