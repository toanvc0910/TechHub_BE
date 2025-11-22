package com.techhub.app.aiservice.service.impl;

import com.techhub.app.aiservice.dto.request.AiExerciseGenerateRequest;
import com.techhub.app.aiservice.dto.response.AiExerciseGenerationResponse;
import com.techhub.app.aiservice.entity.AiGenerationTask;
import com.techhub.app.aiservice.enums.AiTaskStatus;
import com.techhub.app.aiservice.enums.AiTaskType;
import com.techhub.app.aiservice.repository.AiGenerationTaskRepository;
import com.techhub.app.aiservice.service.AiExerciseService;
import com.techhub.app.aiservice.service.OpenAiGateway;
import com.techhub.app.aiservice.service.VectorService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class AiExerciseServiceImpl implements AiExerciseService {

    private final AiGenerationTaskRepository aiGenerationTaskRepository;
    private final OpenAiGateway openAiGateway;
    private final VectorService vectorService;

    @Override
    @Transactional
    public AiExerciseGenerationResponse generateForLesson(AiExerciseGenerateRequest request) {
        log.info("🤖 Generating AI exercises for lesson: {}", request.getLessonId());

        // 1. Fetch lesson content from Qdrant
        Map<String, Object> lessonData = vectorService.getLesson(request.getLessonId());
        if (lessonData == null) {
            throw new RuntimeException("Lesson content not found in AI index. Please ask admin to reindex lessons.");
        }

        String lessonTitle = (String) lessonData.get("title");
        String lessonContent = (String) lessonData.get("content");

        // 2. Create draft task
        AiGenerationTask task = new AiGenerationTask();
        task.setTaskType(AiTaskType.EXERCISE_GENERATION);
        task.setStatus(AiTaskStatus.DRAFT);
        task.setTargetReference(request.getLessonId().toString()); // Save lesson_id để query sau
        task.setRequestPayload(request);

        // Build prompt with fetched content
        String prompt = buildPrompt(request, lessonTitle, lessonContent);
        task.setPrompt(prompt);

        aiGenerationTaskRepository.save(task);

        try {
            // 3. Call OpenAI
            Object aiResponse = openAiGateway.generateStructuredJson(prompt, request);

            // 4. Save result as DRAFT - chờ admin approve
            task.setResultPayload(aiResponse);
            task.setStatus(AiTaskStatus.DRAFT);
            aiGenerationTaskRepository.save(task);

            return AiExerciseGenerationResponse.builder()
                    .taskId(task.getId())
                    .status(AiTaskStatus.DRAFT)
                    .drafts(aiResponse)
                    .message("Exercise draft created successfully. Admin can review and approve.")
                    .build();

        } catch (Exception e) {
            log.error("❌ AI generation failed", e);
            task.setStatus(AiTaskStatus.FAILED);
            task.setResultPayload(Map.of("error", e.getMessage()));
            aiGenerationTaskRepository.save(task);
            throw new RuntimeException("AI generation failed: " + e.getMessage(), e);
        }
    }

    private String buildPrompt(AiExerciseGenerateRequest request, String title, String content) {
        return String.format(
                "Generate %d %s exercises for the lesson '%s'.\n\n" +
                        "Lesson Content:\n%s\n\n" +
                        "Difficulty: %s\n" +
                        "Requirements:\n" +
                        "1. Return a JSON object with key 'exercises' containing an array.\n" +
                        "2. Each exercise must have: type (MCQ/CODING/ESSAY), question, options (for MCQ), testCases (for CODING), explanation.\n"
                        +
                        "3. For MCQ: options is array of strings.\n" +
                        "4. For CODING: testCases is array of {input, expectedOutput}.\n" +
                        "5. All questions must relate directly to the lesson content.\n",
                request.getCount(),
                request.getType(),
                title,
                content != null ? content : "No text content available.",
                request.getDifficulty());
    }
}
