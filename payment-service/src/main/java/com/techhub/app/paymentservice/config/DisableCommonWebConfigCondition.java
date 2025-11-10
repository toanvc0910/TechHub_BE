package com.techhub.app.paymentservice.config;

import com.techhub.app.commonservice.config.CommonWebConfig;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * This configuration ensures CommonWebConfig is not loaded in payment-service
 */
@Configuration
public class DisableCommonWebConfigCondition {

    @Bean
    @ConditionalOnMissingBean(name = "paymentServiceWebConfig")
    public WebMvcConfigurer paymentServiceWebConfig() {
        // This bean prevents CommonWebConfig from being loaded
        return new WebMvcConfigurer() {
            // Empty implementation - no interceptors
        };
    }
}

