package com.techhub.app.proxyclient.config;

import feign.RequestInterceptor;
import feign.RequestTemplate;
import feign.codec.Encoder;
import feign.form.spring.SpringFormEncoder;
import org.springframework.beans.factory.ObjectFactory;
import org.springframework.boot.autoconfigure.http.HttpMessageConverters;
import org.springframework.cloud.openfeign.support.SpringEncoder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

@Configuration
public class FeignConfig {
    private static final List<String> ROLE_PRIORITY = List.of(
            "SUPER_ADMIN",
            "ADMIN",
            "STAFF",
            "INSTRUCTOR",
            "LEARNER",
            "USER");

    @Bean
    public Encoder feignFormEncoder(ObjectFactory<HttpMessageConverters> messageConverters) {
        return new SpringFormEncoder(new SpringEncoder(messageConverters));
    }

    @Bean
    public RequestInterceptor requestInterceptor() {
        return new RequestInterceptor() {
            @Override
            public void apply(RequestTemplate template) {
                ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder
                        .getRequestAttributes();
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
                        String forwardedRole = selectForwardedRole(roles);
                        if (forwardedRole != null) {
                            template.header("X-User-Roles", forwardedRole);
                        }
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

                    // Forward the security level resolved from the DB policy so
                    // downstream services trust it instead of hardcoding public paths.
                    Object securityLevel = request.getAttribute("securityLevel");
                    if (securityLevel != null) {
                        template.header("X-Security-Level", securityLevel.toString());
                    }

                    // Add trace header for debugging
                    template.header("X-Request-Source", "proxy-client");
                }
            }
        };
    }

    private static String selectForwardedRole(List<String> roles) {
        if (roles == null || roles.isEmpty()) {
            return null;
        }
        for (String priorityRole : ROLE_PRIORITY) {
            for (String role : roles) {
                if (priorityRole.equalsIgnoreCase(role)) {
                    return priorityRole;
                }
            }
        }
        return roles.get(0);
    }
}
