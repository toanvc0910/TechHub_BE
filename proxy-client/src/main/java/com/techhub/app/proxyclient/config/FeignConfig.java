package com.techhub.app.proxyclient.config;

import feign.RequestInterceptor;
import feign.RequestTemplate;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;

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

                    // Forward Authorization header
                    String authHeader = request.getHeader("Authorization");
                    if (authHeader != null) {
                        template.header("Authorization", authHeader);
                    }

                    // Forward other important headers
                    String userAgent = request.getHeader("User-Agent");
                    if (userAgent != null) {
                        template.header("User-Agent", userAgent);
                    }

                    String xForwardedFor = request.getHeader("X-Forwarded-For");
                    if (xForwardedFor != null) {
                        template.header("X-Forwarded-For", xForwardedFor);
                    }
                }
            }
        };
    }
}
