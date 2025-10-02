package com.techhub.app.userservice.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Security configuration for User Service
 * Disables CSRF and allows auth endpoints to be accessed without authentication
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
            // Public auth endpoints
            .antMatchers("/api/auth/**").permitAll()
            // Public user registration
            .antMatchers("POST", "/api/users").permitAll()
            // Public password reset endpoints
            .antMatchers("/api/users/forgot-password", "/api/users/reset-password/**").permitAll()
            // Public OAuth2 endpoints
            .antMatchers("/oauth2/**").permitAll()
            // Health check and monitoring
            .antMatchers("/actuator/**", "/health").permitAll()
            // API documentation
            .antMatchers("/swagger-ui/**", "/v3/api-docs/**").permitAll()
            // All other endpoints require authentication (handled by proxy-client)
            .anyRequest().authenticated();

        return http.build();
    }
}
