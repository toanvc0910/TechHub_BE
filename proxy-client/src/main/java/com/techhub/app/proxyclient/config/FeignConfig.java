package com.techhub.app.proxyclient.config;

import feign.RequestInterceptor;
import feign.RequestTemplate;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

@Configuration
public class FeignConfig {

    @Bean
    public RequestInterceptor requestInterceptor() {
        return new RequestInterceptor() {
            @Override
            public void apply(RequestTemplate template) {
                ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
                if (attributes != null) {
                    HttpServletRequest request = attributes.getRequest();

                    // Forward user context via custom headers (NOT JWT token)
                    Object userId = request.getAttribute("userId");
                    if (userId != null) {
                        template.header("X-User-Id", userId.toString());
                    }

                    Object userEmail = request.getAttribute("userEmail");
                    if (userEmail != null) {
                        template.header("X-User-Email", userEmail.toString());
                    }

                    Object userRoles = request.getAttribute("userRoles");
                    if (userRoles != null) {
                        @SuppressWarnings("unchecked")
                        List<String> roles = (List<String>) userRoles;
                        template.header("X-User-Roles", String.join(",", roles));
                    }

                    // Forward other important headers for tracing
                    String userAgent = request.getHeader("User-Agent");
                    if (userAgent != null) {
                        template.header("User-Agent", userAgent);
                    }

                    String xForwardedFor = request.getHeader("X-Forwarded-For");
                    if (xForwardedFor != null) {
                        template.header("X-Forwarded-For", xForwardedFor);
                    }

                    // Add trace header for debugging
                    template.header("X-Request-Source", "proxy-client");
                }
            }
        };
    }
}
