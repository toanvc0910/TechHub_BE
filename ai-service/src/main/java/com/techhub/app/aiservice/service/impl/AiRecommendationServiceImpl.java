package com.techhub.app.aiservice.service.impl;

import com.techhub.app.aiservice.dto.request.RecommendationRequest;
import com.techhub.app.aiservice.dto.response.RecommendationResponse;
import com.techhub.app.aiservice.entity.AiGenerationTask;
import com.techhub.app.aiservice.entity.Recommendation;
import com.techhub.app.aiservice.enums.AiTaskStatus;
import com.techhub.app.aiservice.enums.AiTaskType;
import com.techhub.app.aiservice.enums.RecommendationMode;
import com.techhub.app.aiservice.repository.AiGenerationTaskRepository;
import com.techhub.app.aiservice.repository.RecommendationRepository;
import com.techhub.app.aiservice.service.AiRecommendationService;
import com.techhub.app.aiservice.service.OpenAiGateway;
import com.techhub.app.aiservice.service.VectorService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.StringJoiner;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AiRecommendationServiceImpl implements AiRecommendationService {

    private final AiGenerationTaskRepository aiGenerationTaskRepository;
    private final RecommendationRepository recommendationRepository;
    private final OpenAiGateway openAiGateway;
    private final VectorService vectorService;

    @Override
    @Transactional
    public RecommendationResponse generateRecommendations(RecommendationRequest request) {
        AiTaskType taskType = request.getMode() == RecommendationMode.SCHEDULED
                ? AiTaskType.RECOMMENDATION_SCHEDULED
                : AiTaskType.RECOMMENDATION_REALTIME;

        AiGenerationTask task = new AiGenerationTask();
        task.setTaskType(taskType);
        task.setStatus(AiTaskStatus.PENDING);
        task.setTargetReference(request.getUserId().toString());
        task.setRequestPayload(request);
        aiGenerationTaskRepository.save(task);

        // Step 1: Search similar courses using Qdrant vector search
        log.info("🔍 Searching for recommended courses using Qdrant for user {}", request.getUserId());
        List<Map<String, Object>> similarCourses = vectorService.searchCourses(
                buildUserQuery(request),
                20 // Get top 20 similar courses
        );

        // Step 2: Build prompt with Qdrant results
        String prompt = buildPromptWithContext(request, similarCourses);
        task.setPrompt(prompt);
        aiGenerationTaskRepository.save(task);

        // Step 3: Ask OpenAI to rank and explain recommendations
        Object aiResponse = openAiGateway.generateStructuredJson(prompt, request);
        task.setResultPayload(aiResponse);
        task.setStatus(AiTaskStatus.COMPLETED);
        aiGenerationTaskRepository.save(task);

        Recommendation recommendation = new Recommendation();
        recommendation.setUserId(request.getUserId());
        recommendation.setRecommendedCourses(aiResponse);
        recommendation.setRecommendedPaths(aiResponse);
        recommendationRepository.save(recommendation);

        return RecommendationResponse.builder()
                .recommendationId(recommendation.getId())
                .mode(request.getMode())
                .status(AiTaskStatus.COMPLETED)
                .courses(recommendation.getRecommendedCourses())
                .paths(recommendation.getRecommendedPaths())
                .build();
    }

    private String buildUserQuery(RecommendationRequest request) {
        // Build a query string from user preferences
        StringJoiner query = new StringJoiner(" ");
        if (request.getPreferredLanguages() != null && !request.getPreferredLanguages().isEmpty()) {
            query.add("Programming languages: " + String.join(", ", request.getPreferredLanguages()));
        }
        if (request.getLanguage() != null) {
            query.add("Course language: " + request.getLanguage());
        }
        return query.toString();
    }

    private String buildPromptWithContext(RecommendationRequest request, List<Map<String, Object>> courses) {
        StringJoiner joiner = new StringJoiner("\n");
        joiner.add("Bạn là hệ thống gợi ý khóa học của TechHub.");
        joiner.add("Dựa trên danh sách khóa học tìm được từ semantic search, hãy sắp xếp và gợi ý phù hợp nhất.");
        joiner.add("Sinh JSON gồm recommended_courses (array) và recommended_paths (array).");
        joiner.add("");
        joiner.add("=== User Preferences ===");
        joiner.add("- User ID: " + request.getUserId());
        joiner.add("- Mode: " + request.getMode());
        joiner.add("- Language: " + request.getLanguage());
        joiner.add("- Exclude Course IDs: " + request.getExcludeCourseIds());
        joiner.add("- Preferred Languages: " + request.getPreferredLanguages());
        joiner.add("");
        joiner.add("=== Available Courses from Vector Search ===");

        if (courses.isEmpty()) {
            joiner.add("(Không tìm thấy khóa học phù hợp từ vector search)");
        } else {
            int count = 0;
            for (Map<String, Object> course : courses) {
                Map<String, Object> payload = (Map<String, Object>) course.get("payload");
                if (payload != null) {
                    joiner.add(String.format("%d. Course: %s", ++count, payload.get("title")));
                    joiner.add("   ID: " + payload.get("course_id"));
                    joiner.add("   Description: " + payload.get("description"));
                    joiner.add("   Level: " + payload.get("level"));
                    joiner.add("   Language: " + payload.get("language"));
                    joiner.add("");
                }
            }
        }

        joiner.add("");
        joiner.add("Hãy chọn và sắp xếp top 5-10 khóa học phù hợp nhất, giải thích tại sao.");
        return joiner.toString();
    }
}
