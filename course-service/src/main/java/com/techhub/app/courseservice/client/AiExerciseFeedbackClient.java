package com.techhub.app.courseservice.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;

@FeignClient(name = "AI-SERVICE")
public interface AiExerciseFeedbackClient {

    @PostMapping("/api/ai/exercises/feedback")
    ResponseEntity<String> generateQuizFeedback(@RequestBody Object request,
            @RequestHeader("X-User-Id") String userId,
            @RequestHeader("X-User-Email") String userEmail,
            @RequestHeader("X-User-Roles") String userRoles,
            @RequestHeader("X-Request-Source") String requestSource);
}
