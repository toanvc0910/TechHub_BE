package com.techhub.app.courseservice.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "AI-SERVICE")
public interface AiExerciseFeedbackClient {

    @PostMapping("/api/ai/exercises/feedback")
    ResponseEntity<String> generateQuizFeedback(@RequestBody Object request);
}
