package com.techhub.app.paymentservice.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Bypass interceptor for payment service
 * This interceptor wraps the request to add required headers
 * It's needed because VNPay endpoints must be publicly accessible
 */
@Component
@Slf4j
public class PaymentServiceBypassInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String uri = request.getRequestURI();

        // For all payment service endpoints, we'll wrap the request to add fake headers
        // that satisfy UserContextInterceptor's requirements
        log.debug("PaymentServiceBypassInterceptor: Processing {} {}",
                  request.getMethod(), uri);

        // Mark that this is from payment service (bypass authentication)
        request.setAttribute("PAYMENT_SERVICE_BYPASS", true);

        return true;
    }
}
