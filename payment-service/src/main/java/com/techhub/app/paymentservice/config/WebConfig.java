package com.techhub.app.paymentservice.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Web configuration for payment service
 * Payment endpoints are public - this interceptor allows all requests
 * 
 * Note: RestTemplate bean đã được define trong RestTemplateConfig
 * với @LoadBalanced
 */
@Configuration
@Order(Ordered.HIGHEST_PRECEDENCE)
@Slf4j
public class WebConfig implements WebMvcConfigurer {

    // RestTemplate bean removed - now using LoadBalanced RestTemplate from
    // RestTemplateConfig

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // Add a bypass interceptor that allows all requests
        registry.addInterceptor(new HandlerInterceptor() {
            @Override
            public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
                log.debug("Payment Service: Allowing public access to {} {}",
                        request.getMethod(), request.getRequestURI());
                // Always return true - allow all requests
                return true;
            }
        }).addPathPatterns("/**").order(Ordered.HIGHEST_PRECEDENCE);

        // Do NOT call super.addInterceptors() - this prevents other interceptors
    }
}
