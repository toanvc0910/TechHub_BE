package com.techhub.app.courseservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.netflix.eureka.EnableEurekaClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@EnableEurekaClient
@EnableFeignClients(basePackages = "com.techhub.app.courseservice.client")
@ComponentScan(basePackages = {
    "com.techhub.app.courseservice",
    "com.techhub.app.commonservice"
})
public class CourseserviceApplication {
	public static void main(String[] args) {
		SpringApplication.run(CourseserviceApplication.class, args);
	}
}
