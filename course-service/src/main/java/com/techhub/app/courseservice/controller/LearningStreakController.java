package com.techhub.app.courseservice.controller;

import com.techhub.app.commonservice.payload.GlobalResponse;
import com.techhub.app.courseservice.dto.response.LearningStreakResponse;
import com.techhub.app.courseservice.service.LearningStreakService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/api/courses/streak")
@RequiredArgsConstructor
public class LearningStreakController {

    private final LearningStreakService learningStreakService;

    @GetMapping
    public ResponseEntity<GlobalResponse<LearningStreakResponse>> getCurrentUserStreak(HttpServletRequest request) {
        LearningStreakResponse response = learningStreakService.getCurrentUserStreak();
        return ResponseEntity.ok(
                GlobalResponse.success("Learning streak retrieved", response)
                        .withPath(request.getRequestURI()));
    }
}
