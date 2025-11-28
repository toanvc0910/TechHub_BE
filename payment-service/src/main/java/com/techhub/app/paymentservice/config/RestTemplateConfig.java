package com.techhub.app.paymentservice.config;

import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

/**
 * Configuration cho RestTemplate với Load Balancing
 * Enables service discovery và client-side load balancing qua Eureka
 */
@Configuration
public class RestTemplateConfig {

    /**
     * Tạo LoadBalanced RestTemplate bean
     * 
     * @LoadBalanced annotation cho phép RestTemplate resolve service names
     *               từ Eureka thay vì hardcode URLs
     * 
     *               Example:
     *               - Thay vì: http://localhost:8082/api/v1/enrollments
     *               - Dùng: http://COURSE-SERVICE/api/v1/enrollments
     * 
     *               Ưu điểm:
     *               - Auto service discovery
     *               - Client-side load balancing
     *               - Automatic failover
     *               - No hardcoded URLs
     */
    @Bean
    @LoadBalanced
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
}
