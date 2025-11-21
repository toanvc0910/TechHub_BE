package com.techhub.app.aiservice.service.impl;

import com.techhub.app.aiservice.dto.request.LearningPathGenerateRequest;
import com.techhub.app.aiservice.dto.response.LearningPathDraftResponse;
import com.techhub.app.aiservice.entity.AiGenerationTask;
import com.techhub.app.aiservice.enums.AiTaskStatus;
import com.techhub.app.aiservice.enums.AiTaskType;
import com.techhub.app.aiservice.repository.AiGenerationTaskRepository;
import com.techhub.app.aiservice.service.LearningPathAiService;
import com.techhub.app.aiservice.service.OpenAiGateway;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.StringJoiner;

@Service
@RequiredArgsConstructor
public class LearningPathAiServiceImpl implements LearningPathAiService {

    private final AiGenerationTaskRepository aiGenerationTaskRepository;
    private final OpenAiGateway openAiGateway;

    @Override
    @Transactional
    public LearningPathDraftResponse generatePath(LearningPathGenerateRequest request) {
        AiGenerationTask task = new AiGenerationTask();
        task.setTaskType(AiTaskType.LEARNING_PATH);
        task.setStatus(AiTaskStatus.PENDING);
        task.setTargetReference(request.getUserId().toString());
        task.setRequestPayload(request);
        task.setPrompt(buildPrompt(request));
        aiGenerationTaskRepository.save(task);

        Object aiResponse = openAiGateway.generateStructuredJson(task.getPrompt(), request);
        task.setResultPayload(aiResponse);
        task.setStatus(AiTaskStatus.COMPLETED);
        aiGenerationTaskRepository.save(task);

        return LearningPathDraftResponse.builder()
                .taskId(task.getId())
                .status(task.getStatus())
                .title("Lộ trình AI đề xuất cho " + request.getGoal())
                .nodes(aiResponse)
                .edges(request.isIncludePositions() ? aiResponse : null)
                .courseIds(request.getPreferredCourseIds())
                .build();
    }

    private String buildPrompt(LearningPathGenerateRequest request) {
        StringJoiner joiner = new StringJoiner("\n");
        joiner.add("Bạn là cố vấn học tập của TechHub.");
        joiner.add("Sinh lộ trình JSON gồm nodes và edges để hiển thị bằng React Flow.");
        joiner.add("Yêu cầu:");
        joiner.add("- Mục tiêu: " + request.getGoal());
        joiner.add("- Thời gian: " + request.getTimeframe());
        joiner.add("- Ngôn ngữ: " + request.getLanguage());
        joiner.add("- Trình độ hiện tại: " + request.getCurrentLevel());
        joiner.add("- Mục tiêu cuối: " + request.getTargetLevel());
        joiner.add("- Bao gồm dự án thực hành: " + request.isIncludeProjects());
        joiner.add("- Danh sách khóa học ưu tiên: " + request.getPreferredCourseIds());
        return joiner.toString();
    }
}
