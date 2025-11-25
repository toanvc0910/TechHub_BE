package com.techhub.app.paymentservice.service;


import com.techhub.app.paymentservice.config.PayPalConfig;
import com.techhub.app.paymentservice.entity.Payment;
import com.techhub.app.paymentservice.entity.PaymentGatewayMapping;
import com.techhub.app.paymentservice.entity.Transaction;
import com.techhub.app.paymentservice.entity.TransactionItem;
import com.techhub.app.paymentservice.entity.enums.PaymentMethod;
import com.techhub.app.paymentservice.entity.enums.PaymentStatus;
import com.techhub.app.paymentservice.entity.enums.TransactionStatus;
import com.techhub.app.paymentservice.repository.PaymentGatewayMappingRepository;
import com.techhub.app.paymentservice.repository.PaymentRepository;
import com.techhub.app.paymentservice.repository.TransactionRepository;
import com.techhub.app.paymentservice.repository.TransactionItemRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.apache.hc.client5.http.fluent.Request;
import org.apache.hc.client5.http.fluent.Response;
import org.apache.hc.core5.http.ContentType;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.util.*;

@Service
@Slf4j
public class PayPalPaymentService {
    private final PayPalConfig config;
    private final TransactionRepository transactionRepository;
    private final PaymentRepository paymentRepository;
    private final PaymentGatewayMappingRepository gatewayMappingRepository;
    private final EnrollmentService enrollmentService;
    private final TransactionItemRepository transactionItemRepository;
    private final ObjectMapper mapper = new ObjectMapper();

    public PayPalPaymentService(PayPalConfig config,
                                TransactionRepository transactionRepository,
                                PaymentRepository paymentRepository,
                                PaymentGatewayMappingRepository gatewayMappingRepository,
                                EnrollmentService enrollmentService,
                                TransactionItemRepository transactionItemRepository) {
        this.config = config;
        this.transactionRepository = transactionRepository;
        this.paymentRepository = paymentRepository;
        this.gatewayMappingRepository = gatewayMappingRepository;
        this.enrollmentService = enrollmentService;
        this.transactionItemRepository = transactionItemRepository;
    }

    // Lấy access token
    public String getAccessToken() throws Exception {
        try {
            String credentials = config.getClientId() + ":" + config.getClientSecret();
            String authHeader = "Basic " + Base64.getEncoder().encodeToString(credentials.getBytes(StandardCharsets.UTF_8));

            log.debug("Requesting PayPal access token from: {}", config.getApiBase() + "/v1/oauth2/token");

            Response httpResponse = Request.post(config.getApiBase() + "/v1/oauth2/token")
                    .addHeader("Authorization", authHeader)
                    .addHeader("Content-Type", "application/x-www-form-urlencoded")
                    .addHeader("Accept", "application/json")
                    .bodyString("grant_type=client_credentials", ContentType.APPLICATION_FORM_URLENCODED)
                    .execute();

            String responseBody = httpResponse.returnContent().asString();
            log.debug("PayPal token response: {}", responseBody);

            Map<String, Object> json = mapper.readValue(responseBody, Map.class);

            if (!json.containsKey("access_token")) {
                log.error("No access_token in response: {}", responseBody);
                throw new Exception("Failed to get PayPal access token: " + responseBody);
            }

            String token = json.get("access_token").toString();
            log.info("Successfully obtained PayPal access token");
            return token;
        } catch (Exception e) {
            log.error("Error getting PayPal access token", e);
            throw new Exception("PayPal authentication failed: " + e.getMessage(), e);
        }
    }

    // Tạo order với transaction tracking
    @Transactional
    public Map<String, Object> createOrder(Double amount, String currency, UUID userId, UUID courseId) throws Exception {
        try {
            log.info("=== Creating PayPal Order ===");
            log.info("Amount: {}, Currency: {}, userId: {}, courseId: {}", amount, currency, userId, courseId);

            // Tạo transaction PENDING trước khi tạo PayPal order
            Transaction transaction = Transaction.builder()
                    .userId(userId)
                    .amount(BigDecimal.valueOf(amount))
                    .status(TransactionStatus.PENDING)
                    .isActive("Y")
                    .build();
            Transaction savedTransaction = transactionRepository.saveAndFlush(transaction);
            log.info("Created pending transaction with ID: {} for PayPal payment", savedTransaction.getId());

            // Tạo TransactionItem với courseId
            if (courseId != null) {
                log.info("Creating TransactionItem for courseId: {}", courseId);
                TransactionItem transactionItem = TransactionItem.builder()
                        .transaction(savedTransaction)
                        .courseId(courseId)
                        .priceAtPurchase(BigDecimal.valueOf(amount))
                        .quantity(1)
                        .isActive("Y")
                        .build();

                TransactionItem savedItem = transactionItemRepository.saveAndFlush(transactionItem);
                log.info("TransactionItem saved successfully with ID: {} for course: {}", savedItem.getId(), courseId);
            } else {
                log.warn("No courseId provided, skipping TransactionItem creation");
            }

            String accessToken = getAccessToken();

            Map<String, Object> order = new HashMap<>();
            order.put("intent", "CAPTURE");

            // Format amount correctly for PayPal
            BigDecimal amountDecimal = BigDecimal.valueOf(amount).setScale(2, RoundingMode.HALF_UP);
            String amountString = amountDecimal.toPlainString();

            Map<String, Object> amountMap = new HashMap<>();
            amountMap.put("currency_code", currency);
            amountMap.put("value", amountString);

            Map<String, Object> purchaseUnit = new HashMap<>();
            purchaseUnit.put("amount", amountMap);
            // Lưu transaction ID vào custom_id để tracking
            purchaseUnit.put("custom_id", transaction.getId().toString());

            List<Map<String, Object>> purchaseUnits = new ArrayList<>();
            purchaseUnits.add(purchaseUnit);
            order.put("purchase_units", purchaseUnits);

            Map<String, Object> applicationContext = new HashMap<>();
            applicationContext.put("return_url", config.getReturnUrl());
            applicationContext.put("cancel_url", config.getCancelUrl());
            order.put("application_context", applicationContext);

            String orderJson = mapper.writeValueAsString(order);
            log.info("Creating PayPal order with amount: {} {} (formatted: {})", amount, currency, amountString);
            log.debug("PayPal order request body: {}", orderJson);

            String responseBody = Request.post(config.getApiBase() + "/v2/checkout/orders")
                    .addHeader("Authorization", "Bearer " + accessToken)
                    .addHeader("Content-Type", "application/json")
                    .addHeader("Accept", "application/json")
                    .bodyString(orderJson, ContentType.APPLICATION_JSON)
                    .execute()
                    .returnContent()
                    .asString();

            log.debug("PayPal create order response: {}", responseBody);

            Map<String, Object> result = mapper.readValue(responseBody, Map.class);

            if (result.containsKey("id")) {
                String paypalOrderId = result.get("id").toString();
                log.info("Successfully created PayPal order: {}", paypalOrderId);

                // Lưu mapping giữa PayPal order ID và transaction ID
                PaymentGatewayMapping mapping = PaymentGatewayMapping.builder()
                        .gatewayOrderId(paypalOrderId)
                        .transactionId(transaction.getId())
                        .gatewayType("PAYPAL")
                        .build();
                gatewayMappingRepository.save(mapping);
                log.info("Saved PayPal order mapping: {} -> transaction: {}", paypalOrderId, transaction.getId());

                // Thêm transaction ID vào result để tracking
                result.put("transaction_id", transaction.getId().toString());
                return result;
            } else {
                log.error("PayPal order creation failed. Response: {}", responseBody);
                throw new Exception("PayPal did not return order ID. Response: " + responseBody);
            }
        } catch (Exception e) {
            log.error("Error creating PayPal order. Amount: {}, Currency: {}", amount, currency, e);
            throw new Exception("Failed to create PayPal order: " + e.getMessage(), e);
        }
    }

    // Backward compatibility - createOrder without userId and courseId (DEPRECATED)
    public Map<String, Object> createOrder(Double amount, String currency) throws Exception {
        throw new IllegalArgumentException("userId and courseId parameters are required for PayPal payment");
    }

    // Xác nhận thanh toán (capture) và lưu lịch sử
    @Transactional
    public Map<String, Object> captureOrder(String orderId) throws Exception {
        try {
            String accessToken = getAccessToken();

            log.debug("Capturing PayPal order: {}", orderId);

            Response httpResponse = Request.post(config.getApiBase() + "/v2/checkout/orders/" + orderId + "/capture")
                    .addHeader("Authorization", "Bearer " + accessToken)
                    .addHeader("Content-Type", "application/json")
                    .addHeader("Accept", "application/json")
                    .execute();

            String responseBody = httpResponse.returnContent().asString();
            log.debug("PayPal capture response: {}", responseBody);

            Map<String, Object> result = mapper.readValue(responseBody, Map.class);
            log.info("Successfully captured PayPal order: {}", orderId);

            // Lưu lịch sử giao dịch
            handlePaymentCapture(result, orderId);

            return result;
        } catch (Exception e) {
            log.error("Error capturing PayPal order: {}", orderId, e);
            throw new Exception("Failed to capture PayPal order: " + e.getMessage(), e);
        }
    }

    @Transactional
    public void handlePaymentCapture(Map<String, Object> captureResult, String orderId) {
        log.info("Processing PayPal payment capture - OrderId: {}", orderId);

        try {
            // Lấy transaction ID từ purchase_units custom_id
            UUID transactionId = extractTransactionIdFromCaptureResult(captureResult);

            if (transactionId == null) {
                log.error("Cannot find transaction ID in PayPal capture result for order: {}", orderId);
                return;
            }

            // Biến transactionId bây giờ là effectively final, có thể dùng trong lambda
            final UUID finalTransactionId = transactionId;

            Transaction transaction = transactionRepository.findById(finalTransactionId)
                    .orElseThrow(() -> new RuntimeException("Transaction not found: " + finalTransactionId));

            log.info("Found transaction: {} with current status: {}", finalTransactionId, transaction.getStatus());

            // Xác định trạng thái thanh toán
            String status = captureResult.get("status") != null ? captureResult.get("status").toString() : "";
            PaymentStatus paymentStatus;
            TransactionStatus transactionStatus;

            if ("COMPLETED".equals(status)) {
                paymentStatus = PaymentStatus.SUCCESS;
                transactionStatus = TransactionStatus.COMPLETED;
                log.info("PayPal payment successful for transaction: {}", finalTransactionId);
            } else {
                paymentStatus = PaymentStatus.FAILED;
                transactionStatus = TransactionStatus.PENDING; // Keep as PENDING if failed
                log.warn("PayPal payment failed for transaction: {}. Status: {}", finalTransactionId, status);
            }

            // Cập nhật transaction status
            transaction.setStatus(transactionStatus);
            transaction.setUpdated(java.time.ZonedDateTime.now());
            Transaction savedTransaction = transactionRepository.saveAndFlush(transaction);

            log.info("Updated transaction: {} to status: {} (saved status: {})",
                    finalTransactionId, transactionStatus, savedTransaction.getStatus());

            // Lưu payment record với gateway response
            Map<String, Object> gatewayResponse = new HashMap<>();
            gatewayResponse.put("orderId", orderId);
            gatewayResponse.put("status", status);
            gatewayResponse.put("captureResult", captureResult);

            // Extract more details for gateway response
            if (captureResult.containsKey("id")) {
                gatewayResponse.put("paypal_order_id", captureResult.get("id"));
            }
            if (captureResult.containsKey("payer") && captureResult.get("payer") instanceof Map) {
                gatewayResponse.put("payer", captureResult.get("payer"));
            }

            Payment payment = Payment.builder()
                    .transaction(savedTransaction)
                    .method(PaymentMethod.PAYPAL) // Fix: Use PAYPAL instead of CREDIT_CARD
                    .status(paymentStatus)
                    .gatewayResponse(gatewayResponse)
                    .isActive("Y")
                    .build();

            Payment savedPayment = paymentRepository.saveAndFlush(payment);
            log.info("Saved payment record: {} for transaction: {} with status: {}",
                    savedPayment.getId(), finalTransactionId, paymentStatus);

            // Tạo enrollment khi thanh toán thành công
            if (paymentStatus == PaymentStatus.SUCCESS) {
                try {
                    log.info("PayPal payment successful, creating enrollments for transaction: {}", finalTransactionId);
                    enrollmentService.createEnrollmentForTransaction(savedTransaction);
                } catch (Exception e) {
                    log.error("Failed to create enrollment for transaction: {}. Error: {}", finalTransactionId, e.getMessage(), e);
                    // Không throw exception ở đây để không ảnh hưởng đến flow thanh toán
                    // Enrollment có thể được tạo lại sau bằng cách khác
                }
            }

        } catch (Exception e) {
            log.error("Error handling PayPal payment capture for orderId: {}", orderId, e);
            throw new RuntimeException("Failed to process PayPal payment capture", e);
        }
    }

    // Helper method to extract transaction ID from capture result
    private UUID extractTransactionIdFromCaptureResult(Map<String, Object> captureResult) {
        log.debug("Extracting transaction ID from PayPal capture result");

        // Phương án 1: Thử lấy từ custom_id trong purchase_units
        if (captureResult.containsKey("purchase_units") && captureResult.get("purchase_units") instanceof List) {
            List<?> purchaseUnits = (List<?>) captureResult.get("purchase_units");
            log.debug("Found {} purchase units", purchaseUnits.size());

            if (!purchaseUnits.isEmpty() && purchaseUnits.get(0) instanceof Map) {
                Map<?, ?> unit = (Map<?, ?>) purchaseUnits.get(0);
                log.debug("Purchase unit keys: {}", unit.keySet());

                if (unit.containsKey("custom_id")) {
                    try {
                        String customId = unit.get("custom_id").toString();
                        log.info("Found custom_id in purchase unit: {}", customId);
                        return UUID.fromString(customId);
                    } catch (IllegalArgumentException e) {
                        log.error("Invalid transaction ID format in custom_id: {}", unit.get("custom_id"), e);
                    }
                } else {
                    log.warn("custom_id not found in purchase unit. Available keys: {}", unit.keySet());
                }
            }
        }

        // Phương án 2 (BACKUP): Lấy từ database mapping table bằng PayPal order ID
        if (captureResult.containsKey("id")) {
            String paypalOrderId = captureResult.get("id").toString();
            log.info("Attempting to find transaction ID from mapping table using PayPal order ID: {}", paypalOrderId);

            Optional<PaymentGatewayMapping> mapping = gatewayMappingRepository.findByGatewayOrderId(paypalOrderId);
            if (mapping.isPresent()) {
                UUID transactionId = mapping.get().getTransactionId();
                log.info("Successfully found transaction ID from mapping: {}", transactionId);
                return transactionId;
            } else {
                log.error("No mapping found for PayPal order ID: {}", paypalOrderId);
            }
        }

        log.error("Failed to extract transaction ID from capture result. Available keys: {}", captureResult.keySet());
        return null;
    }

    @Transactional
    public void handlePaymentCancellation(String orderId) {
        try {
            log.warn("PayPal payment cancelled for order: {}", orderId);
            // Có thể tìm và cập nhật transaction status nếu cần
            // Hiện tại chỉ log, không cập nhật database vì user đã cancel
        } catch (Exception e) {
            log.error("Error handling PayPal payment cancellation for orderId: {}", orderId, e);
        }
    }
}
