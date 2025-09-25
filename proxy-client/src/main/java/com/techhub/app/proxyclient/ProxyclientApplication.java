package com.techhub.app.proxyclient;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.cloud.netflix.eureka.EnableEurekaClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication(exclude = {DataSourceAutoConfiguration.class})
@EnableEurekaClient
@EnableFeignClients
@ComponentScan(basePackages = {
    "com.techhub.app.proxyclient",
    "com.techhub.app.commonservice"
})
public class ProxyclientApplication {

	public static void main(String[] args) {
		SpringApplication.run(ProxyclientApplication.class, args);
	}
}
