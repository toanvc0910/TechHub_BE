package com.techhub.app.aiservice.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
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
    private final ObjectMapper objectMapper;

    @Override
    @Transactional
    public LearningPathDraftResponse generatePath(LearningPathGenerateRequest request) {
        log.info("🤖 Generating learning path for goal: {}", request.getGoal());

        // 1. Search for relevant courses using VectorService (limit to 3 for faster response)
        List<Map<String, Object>> relevantCourses = vectorService.searchCourses(request.getGoal(), 3);

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
            log.info("📦 Raw AI response received");

            // 4. Parse OpenAI response to extract content
            // OpenAI returns: { "choices": [{ "message": { "content": "{JSON string}" } }] }
            if (aiResponse == null) {
                throw new RuntimeException("OpenAI returned null response");
            }
            
            Map<String, Object> openAiResponse = (Map<String, Object>) aiResponse;
            log.info("🔍 Response keys: {}", openAiResponse.keySet());
            
            // Check for error response from OpenAI Gateway
            if (openAiResponse.containsKey("error") && openAiResponse.containsKey("fallback")) {
                String errorMsg = (String) openAiResponse.get("error");
                log.error("❌ OpenAI API error: {}", errorMsg);
                throw new RuntimeException("OpenAI API failed: " + errorMsg + ". This might be due to network issues, rate limits, or Cloudflare blocking. Please try again later or check your API key and network connection.");
            }
            
            Map<String, Object> parsedContent;
            if (openAiResponse.containsKey("choices")) {
                // Extract content from OpenAI response format
                List<Map<String, Object>> choices = (List<Map<String, Object>>) openAiResponse.get("choices");
                if (choices == null || choices.isEmpty()) {
                    log.error("❌ Empty or null choices array in OpenAI response");
                    throw new RuntimeException("Empty choices in OpenAI response");
                }
                
                Map<String, Object> firstChoice = choices.get(0);
                if (firstChoice == null) {
                    throw new RuntimeException("First choice is null in OpenAI response");
                }
                
                Map<String, Object> message = (Map<String, Object>) firstChoice.get("message");
                if (message == null) {
                    throw new RuntimeException("Message is null in first choice");
                }
                
                String contentJson = (String) message.get("content");
                if (contentJson == null || contentJson.trim().isEmpty()) {
                    log.error("❌ Content is null or empty in message");
                    throw new RuntimeException("Empty content in OpenAI message");
                }
                    
                // Log content length and preview
                log.info("📄 Content JSON length: {} chars", contentJson.length());
                if (contentJson.length() > 500) {
                    log.info("📄 Content preview (first 500 chars): {}", contentJson.substring(0, 500));
                    log.info("📄 Content preview (last 500 chars): {}", contentJson.substring(contentJson.length() - 500));
                } else {
                    log.info("📄 Full content: {}", contentJson);
                }
                
                // Check for finish_reason to detect truncation
                String finishReason = (String) firstChoice.get("finish_reason");
                log.info("🏁 Finish reason: {}", finishReason);
                
                if ("length".equals(finishReason)) {
                    log.warn("⚠️ Response was truncated due to max_tokens limit!");
                    throw new RuntimeException("OpenAI response was truncated. Please reduce the number of courses or increase max_tokens.");
                }
                
                log.info("📄 Parsing content JSON from OpenAI...");
                parsedContent = objectMapper.readValue(contentJson, Map.class);
            } else if (openAiResponse.containsKey("title") && openAiResponse.containsKey("courses")) {
                // Already parsed content (mock mode or direct response)
                parsedContent = openAiResponse;
            } else {
                log.error("❌ Invalid OpenAI response format. Keys: {}", openAiResponse.keySet());
                throw new RuntimeException("Invalid OpenAI response format. Expected 'choices' or 'title+courses' but got: " + openAiResponse.keySet());
            }

            log.info("✅ Parsed learning path: title={}, courses={}", 
                parsedContent.get("title"), 
                parsedContent.containsKey("courses") ? ((List)parsedContent.get("courses")).size() : 0);

            // Add metadata for approval process
            parsedContent.put("userId", request.getUserId());
            parsedContent.put("goal", request.getGoal());
            parsedContent.put("duration", request.getDuration());
            parsedContent.put("level", request.getLevel());

            // 5. Save parsed content as DRAFT - chờ admin approve
            task.setResultPayload(parsedContent);
            task.setStatus(AiTaskStatus.DRAFT);
            aiGenerationTaskRepository.save(task);

            // Extract title from parsed content
            String title = parsedContent.containsKey("title")
                    ? (String) parsedContent.get("title")
                    : "Learning Path: " + request.getGoal();

            return LearningPathDraftResponse.builder()
                    .taskId(task.getId())
                    .status(AiTaskStatus.DRAFT)
                    .title(title)
                    .nodes(parsedContent.get("nodes"))
                    .edges(parsedContent.get("edges"))
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
                        "   - 'title': String (max 100 chars) - A catchy title for the learning path\n" +
                        "   - 'description': String (max 200 chars) - Brief description of what learner will achieve\n" +
                        "   - 'skills': Array of strings (max 5 items, each max 50 chars) - Key skills learner will gain\n" +
                        "   - 'courses': Array of objects with {courseId: UUID, title: String, description: String (max 150 chars), thumbnail: String, order: number, positionX: number, positionY: number, isOptional: 'Y'/'N'}\n"
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
                        "6. IMPORTANT: In courses array, copy title and thumbnail from course data above, but KEEP description concise (max 150 chars)\n"
                        +
                        "7. Ensure all courseId values in 'courses', 'layoutEdges', 'nodes', and 'edges' arrays match exactly with IDs from Available Courses\n" +
                        "8. Keep response COMPACT - use short descriptions to avoid token limits\n",
                request.getGoal(),
                request.getDuration(),
                request.getLevel(),
                coursesInfo.toString());
    }
}
