package com.techhub.app.blogservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.netflix.eureka.EnableEurekaClient;

@SpringBootApplication
@EnableEurekaClient
public class BlogserviceApplication {

	public static void main(String[] args) {
		SpringApplication.run(BlogserviceApplication.class, args);
	}

}
