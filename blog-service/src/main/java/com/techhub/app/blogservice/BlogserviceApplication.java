package com.techhub.app.blogservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.netflix.eureka.EnableEurekaClient;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@EnableEurekaClient
@ComponentScan(basePackages = {
    "com.techhub.app.blogservice",
    "com.techhub.app.commonservice"
})
public class BlogserviceApplication {

	public static void main(String[] args) {
		SpringApplication.run(BlogserviceApplication.class, args);
	}

}
