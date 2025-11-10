package com.techhub.app.paymentservice.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web configuration for payment service
 * This configuration has highest priority to override CommonWebConfig from common-service
 * Payment service does not use UserContextInterceptor because VNPay endpoints need to be public
 */
@Configuration
@Order(Ordered.HIGHEST_PRECEDENCE)
@RequiredArgsConstructor
public class WebConfig implements WebMvcConfigurer {

    private final PaymentServiceBypassInterceptor bypassInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // Add bypass interceptor with highest priority (order = -1000)
        // This will run BEFORE UserContextInterceptor and set fake headers
        registry.addInterceptor(bypassInterceptor)
                .addPathPatterns("/**")
                .order(Ordered.HIGHEST_PRECEDENCE);
    }
}
