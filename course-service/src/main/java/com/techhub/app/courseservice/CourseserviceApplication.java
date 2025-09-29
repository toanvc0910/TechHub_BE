package com.techhub.app.courseservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@SpringBootApplication
@EnableDiscoveryClient
public class CourseserviceApplication {
    public static void main(String[] args) {
        SpringApplication.run(CourseserviceApplication.class, args);
    }
}
