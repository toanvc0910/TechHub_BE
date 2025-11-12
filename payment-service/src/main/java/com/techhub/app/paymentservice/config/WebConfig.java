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
 * Payment service does not use UserContextInterceptor because payment endpoints need to be public
 *
 * This WebMvcConfigurer runs FIRST and completely replaces any interceptors from CommonWebConfig
 */
@Configuration
@Order(Ordered.HIGHEST_PRECEDENCE)
@RequiredArgsConstructor
public class WebConfig implements WebMvcConfigurer {

    private final PaymentServiceBypassInterceptor bypassInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // IMPORTANT: We DON'T call super.addInterceptors()
        // We only add our bypass interceptor
        // This effectively disables UserContextInterceptor from CommonWebConfig

        registry.addInterceptor(bypassInterceptor)
                .addPathPatterns("/**")
                .order(Ordered.HIGHEST_PRECEDENCE);

        // DO NOT add any other interceptors here
        // This ensures UserContextInterceptor is NOT registered
    }
}
