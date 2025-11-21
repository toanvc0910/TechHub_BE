package com.techhub.app.aiservice.service;

import com.techhub.app.aiservice.dto.request.RecommendationRequest;
import com.techhub.app.aiservice.dto.response.RecommendationResponse;

public interface AiRecommendationService {

    RecommendationResponse generateRecommendations(RecommendationRequest request);
}
