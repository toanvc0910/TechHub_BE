package com.techhub.app.paymentservice.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Dummy UserContextInterceptor that does nothing
 * This overrides the one from common-service
 * Marked as @Primary to ensure this bean is used instead of the common-service one
 */
@Component
@Primary
@Slf4j
public class DummyUserContextInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        // Always allow requests - no authentication check
        log.debug("DummyUserContextInterceptor: Allowing request {} {}",
                request.getMethod(), request.getRequestURI());
        return true;
    }
}

