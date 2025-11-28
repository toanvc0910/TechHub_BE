package com.techhub.app.aiservice.service;

import com.techhub.app.aiservice.dto.request.AiExerciseGenerateRequest;
import com.techhub.app.aiservice.dto.response.AiExerciseGenerationResponse;

public interface AiExerciseService {

    AiExerciseGenerationResponse generateForLesson(AiExerciseGenerateRequest request);
}
