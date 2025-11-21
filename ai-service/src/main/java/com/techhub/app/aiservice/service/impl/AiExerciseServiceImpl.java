package com.techhub.app.aiservice.service.impl;

import com.techhub.app.aiservice.dto.request.AiExerciseGenerateRequest;
import com.techhub.app.aiservice.dto.response.AiExerciseGenerationResponse;
import com.techhub.app.aiservice.entity.AiGenerationTask;
import com.techhub.app.aiservice.enums.AiTaskStatus;
import com.techhub.app.aiservice.enums.AiTaskType;
import com.techhub.app.aiservice.repository.AiGenerationTaskRepository;
import com.techhub.app.aiservice.service.AiExerciseService;
import com.techhub.app.aiservice.service.OpenAiGateway;
import com.techhub.app.commonservice.exception.BadRequestException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.StringJoiner;

@Service
@RequiredArgsConstructor
@Slf4j
public class AiExerciseServiceImpl implements AiExerciseService {

    private final OpenAiGateway openAiGateway;
    private final AiGenerationTaskRepository aiGenerationTaskRepository;

    @Override
    @Transactional
    public AiExerciseGenerationResponse generateForLesson(AiExerciseGenerateRequest request) {
        if (request.getDifficulties() == null || request.getFormats() == null) {
            throw new BadRequestException("Difficulties and formats are required for generation");
        }

        AiGenerationTask task = new AiGenerationTask();
        task.setTaskType(AiTaskType.EXERCISE_GENERATION);
        task.setStatus(AiTaskStatus.DRAFT);
        task.setTargetReference(request.getLessonId().toString());
        task.setRequestPayload(request);
        task.setPrompt(buildPrompt(request));
        aiGenerationTaskRepository.save(task);

        Object aiResponse = openAiGateway.generateStructuredJson(task.getPrompt(), request);
        task.setResultPayload(aiResponse);
        task.setStatus(AiTaskStatus.COMPLETED);
        aiGenerationTaskRepository.save(task);

        return AiExerciseGenerationResponse.builder()
                .taskId(task.getId())
                .status(task.getStatus())
                .drafts(aiResponse)
                .message("Draft exercises generated and stored. Please review before publishing.")
                .build();
    }

    private String buildPrompt(AiExerciseGenerateRequest request) {
        StringJoiner joiner = new StringJoiner("\n");
        joiner.add("Bạn là trợ lý soạn bài tập cho nền tảng TechHub.");
        joiner.add("Sinh bài tập ở trạng thái draft với JSON { exercises: [] }.");
        joiner.add("Các yêu cầu:");
        joiner.add("- Course ID: " + request.getCourseId());
        joiner.add("- Lesson ID: " + request.getLessonId());
        joiner.add("- Ngôn ngữ: " + request.getLanguage());
        joiner.add("- Định dạng câu hỏi: " + request.getFormats());
        joiner.add("- Độ khó: " + request.getDifficulties());
        joiner.add("- Bao gồm test cases: " + request.isIncludeTestCases());
        if (request.getCustomInstruction() != null) {
            joiner.add("- Hướng dẫn bổ sung: " + request.getCustomInstruction());
        }
        joiner.add("Mỗi bài tập gồm nội dung, đáp án, giải thích, và test cases (nếu là coding).");
        return joiner.toString();
    }
}
