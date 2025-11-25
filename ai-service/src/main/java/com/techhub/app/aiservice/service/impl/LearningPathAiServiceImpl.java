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

            // 4. Extract and enrich response data
            Map<String, Object> responseMap = (Map<String, Object>) aiResponse;

            // Add metadata for approval process
            responseMap.put("userId", request.getUserId());
            responseMap.put("goal", request.getGoal());
            responseMap.put("duration", request.getDuration());
            responseMap.put("level", request.getLevel());

            // 5. Save enriched result as DRAFT - chờ admin approve
            task.setResultPayload(responseMap);
            task.setStatus(AiTaskStatus.DRAFT);
            aiGenerationTaskRepository.save(task);

            // Extract title from response or generate from goal
            String title = responseMap.containsKey("title")
                    ? (String) responseMap.get("title")
                    : "Learning Path: " + request.getGoal();

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
            @SuppressWarnings("unchecked")
            Map<String, Object> payload = course.get("payload") != null ? (Map<String, Object>) course.get("payload")
                    : Map.of();
            coursesInfo.append(String.format(
                    "- ID: %s\n  Title: %s\n  Description: %s\n  Level: %s\n  Thumbnail: %s\n\n",
                    course.get("id"),
                    payload.getOrDefault("title", "N/A"),
                    payload.getOrDefault("description", ""),
                    payload.getOrDefault("level", "N/A"),
                    payload.getOrDefault("thumbnail", "")));
        }

        return String.format(
                "Create a learning path for the goal: '%s'.\n" +
                        "Duration: %s, Level: %s\n\n" +
                        "Available Courses (Select the most relevant ones and order them logically):\n%s\n\n" +
                        "Requirements:\n" +
                        "1. Return a JSON object with these keys:\n" +
                        "   - 'title': String - A catchy title for the learning path\n" +
                        "   - 'description': String - Detailed description of what learner will achieve\n" +
                        "   - 'skills': Array of strings - Key skills learner will gain\n" +
                        "   - 'courses': Array of objects with {courseId: UUID, title: String, description: String, thumbnail: String, order: number, positionX: number, positionY: number, isOptional: 'Y'/'N'}\n"
                        +
                        "   - 'layoutEdges': Array of objects with {source: courseId, target: courseId} for prerequisites\n"
                        +
                        "   - 'nodes': Array of objects (for UI visualization) with {id, type='courseNode', data={label, courseId}, position={x,y}}\n"
                        +
                        "   - 'edges': Array of objects (for UI) with {id, source, target}\n" +
                        "2. CRITICAL: You MUST ONLY use courseId values from the 'Available Courses' list above (the 'ID' field). DO NOT generate new UUIDs.\n"
                        +
                        "3. Order courses from beginner to advanced logically\n" +
                        "4. Position nodes visually (x: 50-800, y: 200-1000, space them 350px horizontally, 250px vertically) for flow diagram\n"
                        +
                        "5. Mark advanced/optional courses with isOptional='Y'\n" +
                        "6. IMPORTANT: In courses array, include title, description, and thumbnail from the course data provided above\n"
                        +
                        "7. Ensure all courseId values in 'courses', 'layoutEdges', 'nodes', and 'edges' arrays match exactly with IDs from Available Courses\n",
                request.getGoal(),
                request.getDuration(),
                request.getLevel(),
                coursesInfo.toString());
    }
}
