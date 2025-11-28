package com.techhub.app.paymentservice.service;

import com.techhub.app.paymentservice.config.VNPAYConfig;
import com.techhub.app.paymentservice.dto.response.VNPayPaymentDTO;
import com.techhub.app.paymentservice.entity.Payment;
import com.techhub.app.paymentservice.entity.Transaction;
import com.techhub.app.paymentservice.entity.TransactionItem;
import com.techhub.app.paymentservice.entity.enums.PaymentMethod;
import com.techhub.app.paymentservice.entity.enums.PaymentStatus;
import com.techhub.app.paymentservice.entity.enums.TransactionStatus;
import com.techhub.app.paymentservice.repository.PaymentRepository;
import com.techhub.app.paymentservice.repository.TransactionRepository;
import com.techhub.app.paymentservice.repository.TransactionItemRepository;
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
    private final EnrollmentService enrollmentService;
    private final TransactionItemRepository transactionItemRepository;

    public VNPayPaymentService(VNPAYConfig vnPayConfig,
            TransactionRepository transactionRepository,
            PaymentRepository paymentRepository,
            EnrollmentService enrollmentService,
            TransactionItemRepository transactionItemRepository) {
        this.vnPayConfig = vnPayConfig;
        this.transactionRepository = transactionRepository;
        this.paymentRepository = paymentRepository;
        this.enrollmentService = enrollmentService;
        this.transactionItemRepository = transactionItemRepository;
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

        // Lấy courseId từ request parameter hoặc attribute
        String courseIdStr = request.getParameter("courseId");
        if (courseIdStr == null || courseIdStr.isEmpty()) {
            courseIdStr = (String) request.getAttribute("courseId");
        }

        log.info("=== Creating VNPay Payment ===");
        log.info("Amount: {}, userId: {}, courseId: {}", amount, userIdStr, courseIdStr);

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

        // Parse courseId
        UUID courseId = null;
        if (courseIdStr != null && !courseIdStr.isEmpty()) {
            try {
                courseId = UUID.fromString(courseIdStr);
                log.info("Processing payment for course: {}", courseId);
            } catch (IllegalArgumentException e) {
                log.error("Invalid courseId format: {}", courseIdStr);
                throw new IllegalArgumentException("Invalid courseId format: " + courseIdStr);
            }
        }

        // Create and save transaction
        log.info("Creating transaction with userId: {}, amount: {}", userId, amount / 100.0);
        Transaction transaction = Transaction.builder()
                .userId(userId)
                .amount(BigDecimal.valueOf(amount / 100.0))
                .status(TransactionStatus.PENDING)
                .isActive("Y")
                .build();

        Transaction savedTransaction = transactionRepository.saveAndFlush(transaction);
        log.info("Transaction saved with ID: {}", savedTransaction.getId());

        // Tạo TransactionItem với courseId
        if (courseId != null) {
            log.info("Creating TransactionItem for courseId: {}", courseId);
            TransactionItem transactionItem = TransactionItem.builder()
                    .transaction(savedTransaction)
                    .courseId(courseId)
                    .priceAtPurchase(BigDecimal.valueOf(amount / 100.0))
                    .quantity(1)
                    .isActive("Y")
                    .build();

            // Save transaction item directly using repository
            TransactionItem savedItem = transactionItemRepository.saveAndFlush(transactionItem);
            log.info("TransactionItem saved successfully with ID: {} for course: {}", savedItem.getId(), courseId);
        } else {
            log.warn("No courseId provided, skipping TransactionItem creation");
        }

        Map<String, String> vnpParamsMap = vnPayConfig.getVNPayConfig();
        vnpParamsMap.put("vnp_Amount", String.valueOf(amount));
        // Sử dụng transaction ID làm vnp_TxnRef để tracking
        vnpParamsMap.put("vnp_TxnRef", transaction.getId().toString());

        if (bankCode != null && !bankCode.isEmpty()) {
            vnpParamsMap.put("vnp_BankCode", bankCode);
        }
        vnpParamsMap.put("vnp_IpAddr", VNPayUtil.getIpAddress(request));

        // build query url
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
            Transaction transaction = transactionRepository.findByIdWithItems(transactionId)
                    .orElseThrow(() -> new RuntimeException("Transaction not found: " + transactionId));

            log.info("Found transaction: {} with current status: {}", transactionId, transaction.getStatus()); // Verify
                                                                                                               // transaction
                                                                                                               // items
                                                                                                               // loaded
            if (transaction.getTransactionItems() != null) {
                log.info("Loaded {} transaction items for transaction: {}",
                        transaction.getTransactionItems().size(), transactionId);
            } else {
                log.warn("Transaction {} has no items!", transactionId);
            } // Xác định trạng thái thanh toán
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

            log.info("Updated transaction: {} to status: {}", transactionId, savedTransaction.getStatus()); // Lưu
                                                                                                            // payment
                                                                                                            // record
                                                                                                            // với
                                                                                                            // gateway
                                                                                                            // response
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
            log.info("✅ Step 5 Complete: Saved payment record {} with status: {}",
                    savedPayment.getId(), paymentStatus);

            // Tạo enrollment khi thanh toán thành công
            if (paymentStatus == PaymentStatus.SUCCESS) {
                log.info("\n" +
                        "================================================================================\n" +
                        "💰 PAYMENT SUCCESSFUL - Starting Enrollment Process\n" +
                        "================================================================================\n" +
                        "Transaction ID: {}\n" +
                        "User ID: {}\n" +
                        "Payment Status: {}\n" +
                        "Transaction Status: {}\n" +
                        "================================================================================\n",
                        transactionId, transaction.getUserId(), paymentStatus, transactionStatus);
                try {

                    // Reload transaction với transactionItems để đảm bảo data mới nhất
                    Transaction transactionForEnrollment = transactionRepository.findByIdWithItems(transactionId)
                            .orElseThrow(() -> new RuntimeException("Transaction not found: " + transactionId));

                    // Verify data
                    if (transactionForEnrollment.getTransactionItems() != null
                            && !transactionForEnrollment.getTransactionItems().isEmpty()) {
                        transactionForEnrollment.getTransactionItems()
                                .forEach(item -> log.debug("   → Will enroll user {} in course {}",
                                        transactionForEnrollment.getUserId(), item.getCourseId()));
                    } else {
                        log.error("❌ Transaction {} has no items for enrollment!", transactionId);
                        throw new RuntimeException("Transaction has no items");
                    }
                    enrollmentService.createEnrollmentForTransaction(transactionForEnrollment);

                } catch (Exception e) {
                    log.error("\n" +
                            "================================================================================\n" +
                            "❌ ENROLLMENT PROCESS FAILED\n" +
                            "================================================================================\n" +
                            "Transaction ID: {}\n" +
                            "Error: {}\n" +
                            "Stack Trace:\n" +
                            "================================================================================\n",
                            transactionId, e.getMessage(), e);
                    // Không throw exception ở đây để không ảnh hưởng đến flow thanh toán
                    // Enrollment có thể được tạo lại sau bằng cách khác
                }
            } else {
                log.warn("\n" +
                        "================================================================================\n" +
                        "⚠️ PAYMENT FAILED - Skipping Enrollment\n" +
                        "================================================================================\n" +
                        "Transaction ID: {}\n" +
                        "Payment Status: {}\n" +
                        "Transaction Status: {}\n" +
                        "Response Code: {}\n" +
                        "================================================================================\n",
                        transactionId, paymentStatus, transactionStatus, vnp_ResponseCode);
            }

        } catch (Exception e) {
            log.error("\n" +
                    "================================================================================\n" +
                    "❌ PAYMENT CALLBACK PROCESSING FAILED\n" +
                    "================================================================================\n" +
                    "Transaction Ref: {}\n" +
                    "Error: {}\n" +
                    "Stack Trace:\n" +
                    "================================================================================\n",
                    vnp_TxnRef, e.getMessage(), e);
            throw new RuntimeException("Failed to process payment callback", e);
        }
    }
}
