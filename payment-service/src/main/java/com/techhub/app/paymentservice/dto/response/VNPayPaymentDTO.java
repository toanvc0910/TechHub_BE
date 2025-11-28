package com.techhub.app.paymentservice.dto.response;
import lombok.Builder;

public abstract class VNPayPaymentDTO {
    @Builder
    public static class VNPayResponse {
        public String code;
        public String message;
        public String paymentUrl;
    }
}
