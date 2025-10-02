package com.techhub.app.proxyclient.security;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http.csrf().disable()
            .sessionManagement().sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            .and()
            .authorizeRequests()
            // Public endpoints - no JWT required
            .antMatchers("/api/auth/**", "/api/proxy/auth/**").permitAll()
            .antMatchers("POST", "/api/users", "/api/proxy/users").permitAll()
            .antMatchers("/api/users/forgot-password", "/api/proxy/users/forgot-password").permitAll()
            .antMatchers("/api/users/reset-password/**", "/api/proxy/users/reset-password/**").permitAll()
            .antMatchers("/actuator/**", "/swagger-ui/**", "/v3/api-docs/**").permitAll()
            .antMatchers("/oauth2/**").permitAll()
            // All other endpoints require JWT authentication
            .anyRequest().authenticated()
            .and()
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
