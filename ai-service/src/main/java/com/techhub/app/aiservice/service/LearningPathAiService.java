package com.techhub.app.aiservice.service;

import com.techhub.app.aiservice.dto.request.LearningPathGenerateRequest;
import com.techhub.app.aiservice.dto.response.LearningPathDraftResponse;

public interface LearningPathAiService {

    LearningPathDraftResponse generatePath(LearningPathGenerateRequest request);
}
