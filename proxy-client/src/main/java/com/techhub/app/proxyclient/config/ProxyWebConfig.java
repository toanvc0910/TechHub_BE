package com.techhub.app.proxyclient.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web configuration for proxy-client
 * This configuration ensures NO interceptors are added to proxy-client
 * since it's the entry point and should not have business-level interceptors
 */
@Configuration
public class ProxyWebConfig implements WebMvcConfigurer {

    // Empty configuration - no interceptors should be added to proxy-client
    // All security is handled by SecurityFilterChain and JwtAuthenticationFilter
}
