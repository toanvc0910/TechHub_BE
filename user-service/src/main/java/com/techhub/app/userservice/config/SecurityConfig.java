package com.techhub.app.userservice.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.beans.factory.annotation.Autowired;
import com.techhub.app.userservice.oauth2.CustomOAuth2UserService;
import com.techhub.app.userservice.oauth2.OAuth2AuthenticationSuccessHandler;
import com.techhub.app.userservice.oauth2.OAuth2AuthenticationFailureHandler;

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

    @Autowired
    private CustomOAuth2UserService customOAuth2UserService;

    @Autowired
    private OAuth2AuthenticationSuccessHandler oAuth2AuthenticationSuccessHandler;

    @Autowired
    private OAuth2AuthenticationFailureHandler oAuth2AuthenticationFailureHandler;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf().disable()
            // OAuth2 handshake needs session; allow if required
            .sessionManagement().sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED)
            .and()
            .authorizeRequests()
                // Public endpoints for OAuth2 flow and health
                .antMatchers("/oauth2/**", "/login/oauth2/**", "/api/oauth2/**", "/actuator/**").permitAll()
                // Allow auth APIs (registration, login via proxy flow)
                .antMatchers("/api/auth/**", "/api/users/forgot-password", "/api/users/reset-password/**").permitAll()
                // Everything else can be accessed; JWT is validated by proxy-client
                .anyRequest().permitAll()
            .and()
            .oauth2Login()
                .authorizationEndpoint().baseUri("/oauth2/authorization").and()
                .redirectionEndpoint().baseUri("/login/oauth2/code/*").and()
                .userInfoEndpoint().userService(customOAuth2UserService).and()
                .successHandler(oAuth2AuthenticationSuccessHandler)
                .failureHandler(oAuth2AuthenticationFailureHandler);

        return http.build();
    }
}
