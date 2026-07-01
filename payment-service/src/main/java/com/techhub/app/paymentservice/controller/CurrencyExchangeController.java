package com.techhub.app.paymentservice.controller;

import com.techhub.app.commonservice.payload.GlobalResponse;
import com.techhub.app.paymentservice.service.CurrencyExchangeService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("api/v1/fx")
@RequiredArgsConstructor
public class CurrencyExchangeController {

    private final CurrencyExchangeService currencyExchangeService;

    @GetMapping("/rate")
    public ResponseEntity<GlobalResponse<Map<String, Object>>> getRate(
            @RequestParam("from") String from,
            @RequestParam("to") String to) {
        try {
            BigDecimal rate = currencyExchangeService.getRate(from, to);
            Map<String, Object> body = new HashMap<>();
            body.put("from", from.toUpperCase());
            body.put("to", to.toUpperCase());
            body.put("rate", rate);
            return ResponseEntity.ok(GlobalResponse.success("FX rate", body));
        } catch (Exception ex) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(GlobalResponse.error(ex.getMessage(), HttpStatus.BAD_REQUEST.value()));
        }
    }

    @GetMapping("/convert")
    public ResponseEntity<GlobalResponse<Map<String, Object>>> convert(
            @RequestParam("from") String from,
            @RequestParam("to") String to,
            @RequestParam("amount") BigDecimal amount) {
        try {
            BigDecimal converted = currencyExchangeService.convert(amount, from, to);
            BigDecimal rate = currencyExchangeService.getRate(from, to);
            Map<String, Object> body = new HashMap<>();
            body.put("from", from.toUpperCase());
            body.put("to", to.toUpperCase());
            body.put("amount", amount);
            body.put("rate", rate);
            body.put("converted", converted);
            return ResponseEntity.ok(GlobalResponse.success("FX convert", body));
        } catch (Exception ex) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(GlobalResponse.error(ex.getMessage(), HttpStatus.BAD_REQUEST.value()));
        }
    }
}
