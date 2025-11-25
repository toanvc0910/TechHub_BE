package com.techhub.app.aiservice.config;

import feign.RequestInterceptor;
import feign.RequestTemplate;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;

@Slf4j
@Component
public class FeignClientInterceptor implements RequestInterceptor {

    @Override
    public void apply(RequestTemplate template) {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();

        if (attributes != null) {
            HttpServletRequest request = attributes.getRequest();

            // Forward Authorization header
            String authorization = request.getHeader("Authorization");
            if (authorization != null) {
                template.header("Authorization", authorization);
                log.debug("✅ Forwarding Authorization header to Feign client");
            }

            // Forward X-User-Id header if exists
            String userId = request.getHeader("X-User-Id");
            if (userId != null) {
                template.header("X-User-Id", userId);
                log.debug("✅ Forwarding X-User-Id header to Feign client");
            }

            // Forward Content-Type header
            String contentType = request.getHeader("Content-Type");
            if (contentType != null) {
                template.header("Content-Type", contentType);
                log.debug("✅ Forwarding Content-Type header to Feign client: {}", contentType);
            } else {
                // Set default Content-Type for POST/PUT requests
                if ("POST".equalsIgnoreCase(template.method()) || "PUT".equalsIgnoreCase(template.method())) {
                    template.header("Content-Type", "application/json");
                    log.debug("✅ Setting default Content-Type: application/json");
                }
            }
        } else {
            log.warn("⚠️ No request attributes found - cannot forward headers");
        }
    }
}
