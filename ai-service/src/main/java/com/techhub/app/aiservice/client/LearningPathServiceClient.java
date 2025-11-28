package com.techhub.app.aiservice.client;

import com.techhub.app.aiservice.config.FeignConfiguration;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.UUID;

@FeignClient(name = "LEARNING-PATH-SERVICE", configuration = FeignConfiguration.class)
public interface LearningPathServiceClient {

    @PostMapping("/api/v1/learning-paths")
    ResponseEntity<String> createLearningPath(@RequestBody Object request);

    @PostMapping("/api/v1/learning-paths/{pathId}/courses")
    ResponseEntity<String> addCoursesToPath(
            @PathVariable("pathId") UUID pathId,
            @RequestBody Object request);
}
