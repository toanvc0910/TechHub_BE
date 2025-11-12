package com.techhub.app.paymentservice.controller;

import com.techhub.app.paymentservice.service.PayPalPaymentService;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("api/v1/payment/paypal")
public class PayPalPaymentController {

    private final PayPalPaymentService payPalService;

    public PayPalPaymentController(PayPalPaymentService payPalService) {
        this.payPalService = payPalService;
    }

    @PostMapping("/create")
    public Map<String, Object> createOrder(@RequestParam Double amount) throws Exception {
        return payPalService.createOrder(amount, "USD");
    }

    @GetMapping("/success")
    public String success(@RequestParam String token) throws Exception {
        Map<String, Object> result = payPalService.captureOrder(token);
        return "Thanh toán thành công! Thông tin: " + result;
    }

    @GetMapping("/cancel")
    public String cancel() {
        return "Thanh toán bị hủy.";
    }
}