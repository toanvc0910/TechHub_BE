package com.techhub.app.proxyclient;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication
@EnableFeignClients
public class ProxyclientApplication {

    public static void main(String[] args) {
        SpringApplication.run(ProxyclientApplication.class, args);
    }
}
