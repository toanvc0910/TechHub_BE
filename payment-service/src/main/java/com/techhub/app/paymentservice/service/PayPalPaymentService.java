package com.techhub.app.paymentservice.service;


import com.techhub.app.paymentservice.config.PayPalConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
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
    private final ObjectMapper mapper = new ObjectMapper();

    public PayPalPaymentService(PayPalConfig config) {
        this.config = config;
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

    // Tạo order
    public Map<String, Object> createOrder(Double amount, String currency) throws Exception {
        String accessToken = null;
        try {
            accessToken = getAccessToken();

            Map<String, Object> order = new HashMap<>();
            order.put("intent", "CAPTURE");

            // Format amount correctly for PayPal (use dot as decimal separator, US format)
            BigDecimal amountDecimal = BigDecimal.valueOf(amount).setScale(2, RoundingMode.HALF_UP);
            String amountString = amountDecimal.toPlainString(); // Always uses dot as decimal separator

            Map<String, Object> amountMap = new HashMap<>();
            amountMap.put("currency_code", currency);
            amountMap.put("value", amountString);

            Map<String, Object> purchaseUnit = new HashMap<>();
            purchaseUnit.put("amount", amountMap);

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
                log.info("Successfully created PayPal order: {}", result.get("id"));
                return result;
            } else {
                log.error("PayPal order creation failed. Response: {}", responseBody);
                throw new Exception("PayPal did not return order ID. Response: " + responseBody);
            }
        } catch (Exception e) {
            log.error("Error creating PayPal order. Amount: {}, Currency: {}", amount, currency);
            log.error("Exception details: ", e);

            // Try to extract error details from exception message
            String errorMsg = e.getMessage();
            if (errorMsg != null && errorMsg.contains("{")) {
                log.error("PayPal error response: {}", errorMsg);
            }

            throw new Exception("Failed to create PayPal order: " + e.getMessage(), e);
        }
    }

    // Xác nhận thanh toán (capture)
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
            return result;
        } catch (Exception e) {
            log.error("Error capturing PayPal order: {}", orderId, e);
            throw new Exception("Failed to capture PayPal order: " + e.getMessage(), e);
        }
    }
}
