package com.techhub.app.learningpathservice.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

import java.util.Optional;
import java.util.UUID;

@Configuration
@EnableJpaAuditing
public class JpaConfig {

    @Bean
    public AuditorAware<UUID> auditorProvider() {
        return () -> {
            // TODO: Get current user from SecurityContext
            // For now, return empty Optional
            return Optional.empty();
        };
    }
}
