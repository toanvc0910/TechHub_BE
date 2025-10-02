package com.techhub.app.learningpathservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.netflix.eureka.EnableEurekaClient;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@EnableEurekaClient
@ComponentScan(basePackages = {
    "com.techhub.app.learningpathservice",
    "com.techhub.app.commonservice"
})
public class LearningpathserviceApplication {

	public static void main(String[] args) {
		SpringApplication.run(LearningpathserviceApplication.class, args);
	}

}
