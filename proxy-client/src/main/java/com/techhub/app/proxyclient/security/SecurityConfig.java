package com.techhub.app.proxyclient.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;

@Configuration
@EnableWebSecurity
public class SecurityConfig extends WebSecurityConfigurerAdapter {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOriginPatterns(Arrays.asList("*"));
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(Arrays.asList("*"));
        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    @Override
    protected void configure(HttpSecurity http) throws Exception {
        http.cors().and()
            .csrf().disable()
            .sessionManagement().sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            .and()
            .authorizeRequests()
            // Allow user registration endpoints (both direct and through proxy)
            .antMatchers("POST", "/api/users").permitAll()
            .antMatchers("POST", "/api/proxy/users").permitAll()
            .antMatchers("POST", "/api/proxy/auth/register").permitAll()

            // Allow /api/auth/** and /api/proxy/auth/** for login and authentication
            .antMatchers("/api/auth/**").permitAll()
            .antMatchers("/api/proxy/auth/**").permitAll()

            // Allow checking user existence without authentication
            .antMatchers("/api/users/exists/**").permitAll()
            .antMatchers("/api/proxy/users/exists/**").permitAll()

            // Health check and actuator endpoints
            .antMatchers("/actuator/**").permitAll()

            // Swagger endpoints
            .antMatchers("/swagger-ui/**", "/v3/api-docs/**").permitAll()

            // For API Gateway routing - allow /app/** paths with proper mapping
            .antMatchers("POST", "/app/api/users").permitAll()
            .antMatchers("POST", "/app/api/proxy/users").permitAll()
            .antMatchers("POST", "/app/api/proxy/auth/register").permitAll()
            .antMatchers("/app/api/auth/**").permitAll()
            .antMatchers("/app/api/proxy/auth/**").permitAll()
            .antMatchers("/app/api/users/exists/**").permitAll()
            .antMatchers("/app/api/proxy/users/exists/**").permitAll()

            // All other requests require authentication
            .anyRequest().authenticated();
    }
}
