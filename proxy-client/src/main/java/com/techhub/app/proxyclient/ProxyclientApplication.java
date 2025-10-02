package com.techhub.app.proxyclient;

import com.techhub.app.commonservice.jwt.JwtUtil;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.cloud.netflix.eureka.EnableEurekaClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication(exclude = {DataSourceAutoConfiguration.class})
@EnableEurekaClient
@EnableFeignClients
@ComponentScan(basePackages = {
    "com.techhub.app.proxyclient"  // Only scan proxy-client package
})
public class ProxyclientApplication {

    // Manually define JwtUtil bean instead of scanning
    @Bean
    public JwtUtil jwtUtil() {
        return new JwtUtil();
    }

    public static void main(String[] args) {
        SpringApplication.run(ProxyclientApplication.class, args);
    }
}
