package com.techhub.app.aiservice.controller;

import com.techhub.app.aiservice.dto.request.LearningPathGenerateRequest;
import com.techhub.app.aiservice.dto.response.LearningPathDraftResponse;
import com.techhub.app.aiservice.service.LearningPathAiService;
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
@RequestMapping("/api/ai/learning-paths")
@Validated
@RequiredArgsConstructor
public class LearningPathAiController {

    private final LearningPathAiService learningPathAiService;

    @PostMapping("/generate")
    public ResponseEntity<GlobalResponse<LearningPathDraftResponse>> generate(
            @Valid @RequestBody LearningPathGenerateRequest request,
            HttpServletRequest servletRequest) {

        LearningPathDraftResponse response = learningPathAiService.generatePath(request);
        return ResponseEntity.ok(
                GlobalResponse.success("Learning path draft generated", response)
                        .withStatus("AI_LEARNING_PATH_DRAFT")
                        .withPath(servletRequest.getRequestURI())
        );
    }
}
