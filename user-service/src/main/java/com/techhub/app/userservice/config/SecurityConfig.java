package com.techhub.app.userservice.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Security configuration for User Service
 *
 * IMPORTANT: User Service does NOT validate JWT tokens directly.
 * JWT validation is handled by proxy-client (API Gateway).
 * This service receives user info via headers: X-User-Id, X-User-Email, X-User-Roles
 *
 * All endpoints use permitAll() because:
 * 1. Proxy-client already validated JWT token
 * 2. User-service trusts proxy-client and reads headers
 * 3. Business logic in controllers checks X-User-Id header for authorization
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http.csrf().disable()
            .sessionManagement().sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            .and()
            .authorizeRequests()
            // All endpoints are permitted because proxy-client handles JWT validation
            // Controllers will check X-User-Id header for business authorization
            .anyRequest().permitAll();

        return http.build();
    }
}
