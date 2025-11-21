package com.techhub.app.aiservice.controller;

import com.techhub.app.aiservice.dto.request.AiExerciseGenerateRequest;
import com.techhub.app.aiservice.dto.response.AiExerciseGenerationResponse;
import com.techhub.app.aiservice.service.AiExerciseService;
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
@RequestMapping("/api/ai/exercises")
@Validated
@RequiredArgsConstructor
public class AiExerciseController {

    private final AiExerciseService aiExerciseService;

    @PostMapping("/generate")
    public ResponseEntity<GlobalResponse<AiExerciseGenerationResponse>> generateExercises(
            @Valid @RequestBody AiExerciseGenerateRequest request,
            HttpServletRequest servletRequest) {

        AiExerciseGenerationResponse response = aiExerciseService.generateForLesson(request);
        return ResponseEntity.ok(
                GlobalResponse.success("AI exercise drafts created", response)
                        .withStatus("AI_EXERCISE_DRAFT")
                        .withPath(servletRequest.getRequestURI())
        );
    }
}
