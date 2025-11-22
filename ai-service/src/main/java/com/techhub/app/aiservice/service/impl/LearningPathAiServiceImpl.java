package com.techhub.app.aiservice.service.impl;

import com.techhub.app.aiservice.dto.request.LearningPathGenerateRequest;
import com.techhub.app.aiservice.dto.response.LearningPathDraftResponse;
import com.techhub.app.aiservice.entity.AiGenerationTask;
import com.techhub.app.aiservice.enums.AiTaskStatus;
import com.techhub.app.aiservice.enums.AiTaskType;
import com.techhub.app.aiservice.repository.AiGenerationTaskRepository;
import com.techhub.app.aiservice.service.LearningPathAiService;
import com.techhub.app.aiservice.service.OpenAiGateway;
import com.techhub.app.aiservice.service.VectorService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class LearningPathAiServiceImpl implements LearningPathAiService {

    private final AiGenerationTaskRepository aiGenerationTaskRepository;
    private final OpenAiGateway openAiGateway;
    private final VectorService vectorService;

    @Override
    @Transactional
    public LearningPathDraftResponse generatePath(LearningPathGenerateRequest request) {
        log.info("🤖 Generating learning path for goal: {}", request.getGoal());

        // 1. Search for relevant courses using VectorService
        List<Map<String, Object>> relevantCourses = vectorService.searchCourses(request.getGoal(), 10);

        if (relevantCourses.isEmpty()) {
            throw new RuntimeException("No relevant courses found for the given goal. Please try different keywords.");
        }

        // 2. Create draft task
        AiGenerationTask task = new AiGenerationTask();
        task.setTaskType(AiTaskType.LEARNING_PATH_GENERATION);
        task.setStatus(AiTaskStatus.DRAFT);
        task.setTargetReference(request.getGoal()); // Save goal để query sau
        task.setRequestPayload(request);

        // Build prompt with relevant courses
        String prompt = buildPrompt(request, relevantCourses);
        task.setPrompt(prompt);

        aiGenerationTaskRepository.save(task);

        try {
            // 3. Call OpenAI
            Object aiResponse = openAiGateway.generateStructuredJson(prompt, request);

            // 4. Save result as DRAFT - chờ admin approve
            task.setResultPayload(aiResponse);
            task.setStatus(AiTaskStatus.DRAFT);
            aiGenerationTaskRepository.save(task);

            // Extract title from request
            String title = "Learning Path: " + request.getGoal();

            Map<String, Object> responseMap = (Map<String, Object>) aiResponse;

            return LearningPathDraftResponse.builder()
                    .taskId(task.getId())
                    .status(AiTaskStatus.DRAFT)
                    .title(title)
                    .nodes(responseMap.get("nodes"))
                    .edges(responseMap.get("edges"))
                    .build();

        } catch (Exception e) {
            log.error("❌ Learning path generation failed", e);
            task.setStatus(AiTaskStatus.FAILED);
            task.setResultPayload(Map.of("error", e.getMessage()));
            aiGenerationTaskRepository.save(task);
            throw new RuntimeException("Learning path generation failed: " + e.getMessage(), e);
        }
    }

    private String buildPrompt(LearningPathGenerateRequest request, List<Map<String, Object>> courses) {
        StringBuilder coursesInfo = new StringBuilder();
        for (Map<String, Object> course : courses) {
            coursesInfo.append(String.format("- ID: %s, Title: %s, Level: %s\n",
                    course.get("id"),
                    course.get("payload") != null ? ((Map) course.get("payload")).get("title") : "N/A",
                    course.get("payload") != null ? ((Map) course.get("payload")).get("level") : "N/A"));
        }

        return String.format(
                "Create a learning path for the goal: '%s'.\n" +
                        "Duration: %s, Level: %s\n\n" +
                        "Available Courses (Select the most relevant ones and order them logically):\n%s\n\n" +
                        "Requirements:\n" +
                        "1. Return a JSON object with two keys: 'nodes' and 'edges'.\n" +
                        "2. 'nodes': Array of objects matching 'LearningPathNode' structure (id, type='courseNode', data={label, courseId}, position={x,y}).\n"
                        +
                        "3. 'edges': Array of objects matching 'LearningPathEdge' structure (id, source, target).\n" +
                        "4. Arrange nodes visually using x,y coordinates to form a clear path (e.g., left to right or top to bottom).\n",
                request.getGoal(),
                request.getDuration(),
                request.getLevel(),
                coursesInfo.toString());
    }
}
