package com.techhub.app.aiservice.controller;

import com.techhub.app.aiservice.dto.request.RecommendationRequest;
import com.techhub.app.aiservice.dto.response.RecommendationResponse;
import com.techhub.app.aiservice.enums.RecommendationMode;
import com.techhub.app.aiservice.service.AiRecommendationService;
import com.techhub.app.commonservice.payload.GlobalResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;

@RestController
@RequestMapping("/api/ai/recommendations")
@Validated
@RequiredArgsConstructor
public class RecommendationController {

    private final AiRecommendationService aiRecommendationService;

    @PostMapping("/realtime")
    public ResponseEntity<GlobalResponse<RecommendationResponse>> realtime(
            @Valid @RequestBody RecommendationRequest request,
            HttpServletRequest servletRequest) {

        request.setMode(RecommendationMode.REALTIME);
        RecommendationResponse response = aiRecommendationService.generateRecommendations(request);
        return ResponseEntity.ok(
                GlobalResponse.success("Realtime recommendations generated", response)
                        .withStatus("AI_RECOMMENDATION_REALTIME")
                        .withPath(servletRequest.getRequestURI())
        );
    }

    @PostMapping("/scheduled")
    public ResponseEntity<GlobalResponse<RecommendationResponse>> scheduled(
            @Valid @RequestBody RecommendationRequest request,
            HttpServletRequest servletRequest) {

        request.setMode(RecommendationMode.SCHEDULED);
        RecommendationResponse response = aiRecommendationService.generateRecommendations(request);
        return ResponseEntity.ok(
                GlobalResponse.success("Scheduled recommendations generated", response)
                        .withStatus("AI_RECOMMENDATION_SCHEDULED")
                        .withPath(servletRequest.getRequestURI())
        );
    }
}
