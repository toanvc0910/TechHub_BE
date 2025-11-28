package com.techhub.app.courseservice.service.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.techhub.app.commonservice.context.UserContext;
import com.techhub.app.commonservice.exception.BadRequestException;
import com.techhub.app.commonservice.exception.ForbiddenException;
import com.techhub.app.commonservice.exception.NotFoundException;
import com.techhub.app.commonservice.exception.UnauthorizedException;
import com.techhub.app.courseservice.dto.ExerciseTestCaseDto;
import com.techhub.app.courseservice.dto.request.ExerciseRequest;
import com.techhub.app.courseservice.dto.request.ExerciseSubmissionRequest;
import com.techhub.app.courseservice.dto.request.LessonProgressRequest;
import com.techhub.app.courseservice.dto.response.ExerciseResponse;
import com.techhub.app.courseservice.dto.response.ExerciseSubmissionResponse;
import com.techhub.app.courseservice.dto.response.TestCaseResultResponse;
import com.techhub.app.courseservice.entity.Course;
import com.techhub.app.courseservice.entity.Enrollment;
import com.techhub.app.courseservice.entity.Exercise;
import com.techhub.app.courseservice.entity.ExerciseTestCase;
import com.techhub.app.courseservice.entity.Lesson;
import com.techhub.app.courseservice.entity.Submission;
import com.techhub.app.courseservice.enums.ExerciseType;
import com.techhub.app.courseservice.enums.SubmissionStatus;
import com.techhub.app.courseservice.enums.TestCaseVisibility;
import com.techhub.app.courseservice.repository.CourseRepository;
import com.techhub.app.courseservice.repository.EnrollmentRepository;
import com.techhub.app.courseservice.repository.ExerciseRepository;
import com.techhub.app.courseservice.repository.ExerciseTestCaseRepository;
import com.techhub.app.courseservice.repository.LessonRepository;
import com.techhub.app.courseservice.repository.SubmissionRepository;
import com.techhub.app.courseservice.service.CourseProgressService;
import com.techhub.app.courseservice.service.ExerciseService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class ExerciseServiceImpl implements ExerciseService {

    private final CourseRepository courseRepository;
    private final LessonRepository lessonRepository;
    private final ExerciseRepository exerciseRepository;
    private final ExerciseTestCaseRepository testCaseRepository;
    private final SubmissionRepository submissionRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final CourseProgressService courseProgressService;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional(readOnly = true)
    public ExerciseResponse getLessonExercise(UUID courseId, UUID lessonId) {
        Lesson lesson = resolveLesson(courseId, lessonId);
        Exercise exercise = exerciseRepository.findByLesson_IdAndIsActiveTrue(lesson.getId())
                .orElseThrow(() -> new NotFoundException("Exercise not found for lesson"));

        boolean manager = canManageCourse(lesson.getChapter().getCourse());
        List<ExerciseTestCase> testCases = testCaseRepository
                .findByExercise_IdAndIsActiveTrueOrderByOrderIndexAsc(exercise.getId());

        return mapToResponse(exercise, testCases, manager);
    }

    @Override
    public ExerciseResponse upsertExercise(UUID courseId, UUID lessonId, ExerciseRequest request) {
        Lesson lesson = resolveLesson(courseId, lessonId);
        ensureManagePermission(lesson.getChapter().getCourse());

        Exercise exercise = exerciseRepository.findByLesson_IdAndIsActiveTrue(lesson.getId())
                .orElseGet(() -> {
                    Exercise entity = new Exercise();
                    entity.setLesson(lesson);
                    entity.setOrderIndex(1);
                    entity.setCreatedBy(UserContext.getCurrentUserId());
                    entity.setIsActive(true);
                    return entity;
                });

        exercise.setType(request.getType());
        exercise.setQuestion(request.getQuestion());
        exercise.setOptions(request.getOptions());
        exercise.setUpdatedBy(UserContext.getCurrentUserId());
        exerciseRepository.save(exercise);

        syncTestCases(exercise, request.getTestCases());

        List<ExerciseTestCase> testCases = testCaseRepository
                .findByExercise_IdAndIsActiveTrueOrderByOrderIndexAsc(exercise.getId());
        return mapToResponse(exercise, testCases, true);
    }

    @Override
    public List<ExerciseResponse> getLessonExercises(UUID courseId, UUID lessonId) {
        Lesson lesson = resolveLesson(courseId, lessonId);
        List<Exercise> exercises = exerciseRepository
                .findByLesson_IdAndIsActiveTrueOrderByOrderIndexAsc(lesson.getId());

        return exercises.stream()
                .map(exercise -> {
                    List<ExerciseTestCase> testCases = testCaseRepository
                            .findByExercise_IdAndIsActiveTrueOrderByOrderIndexAsc(exercise.getId());
                    return mapToResponse(exercise, testCases, true);
                })
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public List<ExerciseResponse> createExercises(UUID courseId, UUID lessonId, List<ExerciseRequest> requests) {
        Lesson lesson = resolveLesson(courseId, lessonId);
        ensureManagePermission(lesson.getChapter().getCourse());

        List<ExerciseResponse> responses = new ArrayList<>();
        UUID currentUserId = UserContext.getCurrentUserId();

        for (int i = 0; i < requests.size(); i++) {
            ExerciseRequest request = requests.get(i);

            Exercise exercise = new Exercise();
            exercise.setLesson(lesson);
            exercise.setType(request.getType());
            exercise.setQuestion(request.getQuestion());
            exercise.setOptions(request.getOptions());
            exercise.setOrderIndex(i + 1);
            exercise.setCreatedBy(currentUserId);
            exercise.setUpdatedBy(currentUserId);
            exercise.setIsActive(true);

            exerciseRepository.save(exercise);
            syncTestCases(exercise, request.getTestCases());

            List<ExerciseTestCase> testCases = testCaseRepository
                    .findByExercise_IdAndIsActiveTrueOrderByOrderIndexAsc(exercise.getId());
            responses.add(mapToResponse(exercise, testCases, true));
        }

        return responses;
    }

    @Override
    public ExerciseResponse updateExercise(UUID courseId, UUID lessonId, UUID exerciseId, ExerciseRequest request) {
        log.debug("[updateExercise] Start - courseId={}, lessonId={}, exerciseId={}, request={}", courseId, lessonId,
                exerciseId, request);
        Lesson lesson = resolveLesson(courseId, lessonId);
        log.debug("[updateExercise] Resolved lesson id: {}", lesson.getId());
        ensureManagePermission(lesson.getChapter().getCourse());
        log.debug("[updateExercise] Permission check passed for course {}", lesson.getChapter().getCourse().getId());

        Exercise exercise = exerciseRepository.findById(exerciseId)
                .orElseThrow(() -> new NotFoundException("Exercise not found"));
        log.debug("[updateExercise] Found exercise id: {} (active={})", exercise.getId(), exercise.getIsActive());

        // Verify exercise belongs to the lesson
        if (!exercise.getLesson().getId().equals(lessonId)) {
            log.warn("[updateExercise] Exercise {} does not belong to lesson {}", exerciseId, lessonId);
            throw new ForbiddenException("Exercise does not belong to the specified lesson");
        }
        log.debug("[updateExercise] Exercise belongs to lesson");

        // Verify exercise is active
        if (exercise.getIsActive() != null && !exercise.getIsActive()) {
            log.warn("[updateExercise] Attempt to update inactive exercise {}", exerciseId);
            throw new NotFoundException("Exercise is not active");
        }
        log.debug("[updateExercise] Exercise is active");

        // Update exercise fields
        exercise.setType(request.getType());
        exercise.setQuestion(request.getQuestion());
        exercise.setOptions(request.getOptions());

        if (request.getOrderIndex() != null) {
            log.debug("[updateExercise] Updating orderIndex from {} to {}", exercise.getOrderIndex(),
                    request.getOrderIndex());
            exercise.setOrderIndex(request.getOrderIndex());
        }

        exercise.setUpdatedBy(UserContext.getCurrentUserId());
        exerciseRepository.save(exercise);
        log.debug("[updateExercise] Exercise fields updated and saved. Current orderIndex: {}",
                exercise.getOrderIndex());

        // Sync test cases
        syncTestCases(exercise, request.getTestCases());
        log.debug("[updateExercise] Test cases synchronized");

        List<ExerciseTestCase> testCases = testCaseRepository
                .findByExercise_IdAndIsActiveTrueOrderByOrderIndexAsc(exercise.getId());
        ExerciseResponse response = mapToResponse(exercise, testCases, true);
        log.debug("[updateExercise] Returning response for exercise {}", exerciseId);
        return response;
    }

    @Override
    public void deleteExercise(UUID courseId, UUID lessonId, UUID exerciseId) {
        Lesson lesson = resolveLesson(courseId, lessonId);
        ensureManagePermission(lesson.getChapter().getCourse());
        log.debug("[deleteExercise] Start - courseId={}, lessonId={}, exerciseId={}", courseId, lessonId,
                exerciseId);
        Exercise exercise = exerciseRepository.findById(exerciseId)
                .orElseThrow(() -> new NotFoundException("Exercise not found"));

        // Verify exercise belongs to the lesson
        if (!exercise.getLesson().getId().equals(lessonId)) {
            throw new ForbiddenException("Exercise does not belong to the specified lesson");
        }

        // Soft delete the exercise
        exercise.setIsActive(false);
        exercise.setUpdatedBy(UserContext.getCurrentUserId());
        exerciseRepository.save(exercise);

        // Soft delete all associated test cases
        List<ExerciseTestCase> testCases = testCaseRepository
                .findByExercise_IdAndIsActiveTrueOrderByOrderIndexAsc(exercise.getId());
        for (ExerciseTestCase testCase : testCases) {
            testCase.setIsActive(false);
            testCase.setUpdatedBy(UserContext.getCurrentUserId());
            testCaseRepository.save(testCase);
        }

        log.info("Exercise {} soft deleted by user {}", exerciseId, UserContext.getCurrentUserId());
    }

    @Override
    public ExerciseSubmissionResponse submitExercise(UUID courseId, UUID lessonId, ExerciseSubmissionRequest request) {
        UUID userId = requireCurrentUser();
        Lesson lesson = resolveLesson(courseId, lessonId);
        ensureLearnerOrManager(lesson.getChapter().getCourse(), userId);

        Exercise exercise = exerciseRepository.findByLesson_IdAndIsActiveTrue(lesson.getId())
                .orElseThrow(() -> new NotFoundException("Exercise not found for lesson"));

        Submission submission = new Submission();
        submission.setExercise(exercise);
        submission.setUserId(userId);
        submission.setAnswer(request.getAnswer());
        submission.setSubmissionData(request.getSubmissionData());
        submission.setCreatedBy(userId);
        submission.setUpdatedBy(userId);

        List<ExerciseTestCase> testCases = testCaseRepository
                .findByExercise_IdAndIsActiveTrueOrderByOrderIndexAsc(exercise.getId());
        ExerciseEvaluationResult evaluation = evaluateSubmission(exercise, testCases, request);

        submission.setStatus(evaluation.status());
        submission.setGrade(evaluation.grade());
        submission.setGradedAt(
                evaluation.status() == SubmissionStatus.PASSED || evaluation.status() == SubmissionStatus.FAILED
                        || evaluation.status() == SubmissionStatus.PARTIAL ? OffsetDateTime.now() : null);

        submissionRepository.save(submission);
        log.debug("Submission {} stored with status {}", submission.getId(), submission.getStatus());

        if (evaluation.grade() != null) {
            LessonProgressRequest progressRequest = new LessonProgressRequest();
            float completion = Math.max(0f, Math.min(1f, evaluation.grade() / 100f));
            progressRequest.setCompletion(completion);
            if (completion >= 1f) {
                progressRequest.setMarkComplete(true);
            }
            courseProgressService.updateLessonProgress(courseId, lessonId, progressRequest);
        }

        return ExerciseSubmissionResponse.builder()
                .submissionId(submission.getId())
                .status(submission.getStatus())
                .grade(submission.getGrade())
                .gradedAt(submission.getGradedAt())
                .passed(evaluation.status() == SubmissionStatus.PASSED)
                .testCaseResults(evaluation.testCaseResults())
                .build();
    }

    private ExerciseEvaluationResult evaluateSubmission(Exercise exercise,
            List<ExerciseTestCase> testCases,
            ExerciseSubmissionRequest request) {
        ExerciseType type = exercise.getType();
        switch (type) {
            case MULTIPLE_CHOICE:
                return evaluateMultipleChoice(exercise, request);
            case CODING:
                return evaluateCoding(testCases, request);
            case OPEN_ENDED:
                return new ExerciseEvaluationResult(null, SubmissionStatus.PENDING, List.of());
            default:
                throw new BadRequestException("Unsupported exercise type");
        }
    }

    private ExerciseEvaluationResult evaluateMultipleChoice(Exercise exercise, ExerciseSubmissionRequest request) {
        try {
            Map<String, Object> optionPayload = objectMapper.convertValue(
                    exercise.getOptions() != null ? exercise.getOptions() : Map.of(),
                    new TypeReference<Map<String, Object>>() {
                    });
            List<Map<String, Object>> choices = (List<Map<String, Object>>) optionPayload.getOrDefault("choices",
                    List.of());

            Set<String> correctIds = choices.stream()
                    .filter(choice -> Boolean.TRUE.equals(choice.get("correct")))
                    .map(choice -> String.valueOf(choice.get("id")))
                    .collect(Collectors.toSet());

            List<String> answers = objectMapper.readValue(request.getAnswer(), new TypeReference<List<String>>() {
            });
            Set<String> submitted = new HashSet<>(answers);

            boolean passed = submitted.equals(correctIds);
            float grade = passed ? 100f : 0f;

            TestCaseResultResponse result = TestCaseResultResponse.builder()
                    .testCaseId(null)
                    .passed(passed)
                    .input(null)
                    .expectedOutput(String.join(",", correctIds))
                    .actualOutput(String.join(",", submitted))
                    .visibility(TestCaseVisibility.PUBLIC)
                    .weight(1f)
                    .build();

            SubmissionStatus status = passed ? SubmissionStatus.PASSED : SubmissionStatus.FAILED;
            return new ExerciseEvaluationResult(grade, status, List.of(result));
        } catch (IOException | IllegalArgumentException e) {
            throw new BadRequestException("Invalid answer format for multiple choice exercise");
        }
    }

    private ExerciseEvaluationResult evaluateCoding(List<ExerciseTestCase> testCases,
            ExerciseSubmissionRequest request) {
        Map<String, Object> payload;
        Map<String, String> outputs;
        try {
            payload = objectMapper.convertValue(request.getSubmissionData(), new TypeReference<Map<String, Object>>() {
            });
            outputs = objectMapper.convertValue(
                    payload.getOrDefault("outputs", Map.of()),
                    new TypeReference<Map<String, String>>() {
                    });
        } catch (IllegalArgumentException ex) {
            throw new BadRequestException("Invalid submission data for coding exercise");
        }

        List<TestCaseResultResponse> results = new ArrayList<>();
        float totalWeight = 0f;
        float earnedWeight = 0f;

        for (ExerciseTestCase testCase : testCases) {
            float weight = testCase.getWeight() != null ? testCase.getWeight() : 1f;
            totalWeight += weight;

            String key = testCase.getId() != null ? testCase.getId().toString()
                    : String.valueOf(testCase.getOrderIndex());
            String actual = outputs.getOrDefault(key, null);
            String expected = testCase.getExpectedOutput();

            boolean passed = actual != null && expected != null && expected.trim().equals(actual.trim());
            if (passed) {
                earnedWeight += weight;
            }

            results.add(TestCaseResultResponse.builder()
                    .testCaseId(testCase.getId())
                    .passed(passed)
                    .input(testCase.getInput())
                    .expectedOutput(expected)
                    .actualOutput(actual)
                    .visibility(testCase.getVisibility())
                    .weight(weight)
                    .build());
        }

        float grade = totalWeight == 0 ? 0f : (earnedWeight / totalWeight) * 100f;
        SubmissionStatus status;
        if (grade >= 99.9f) {
            status = SubmissionStatus.PASSED;
        } else if (grade > 0f) {
            status = SubmissionStatus.PARTIAL;
        } else {
            status = SubmissionStatus.FAILED;
        }

        return new ExerciseEvaluationResult(grade, status, results);
    }

    private ExerciseResponse mapToResponse(Exercise exercise, List<ExerciseTestCase> testCases,
            boolean includeAllTestCases) {
        List<ExerciseTestCaseDto> testCaseDtos = testCases.stream()
                .filter(testCase -> includeAllTestCases || testCase.getVisibility() == TestCaseVisibility.PUBLIC)
                .map(testCase -> ExerciseTestCaseDto.builder()
                        .id(testCase.getId())
                        .orderIndex(testCase.getOrderIndex())
                        .visibility(testCase.getVisibility())
                        .input(includeAllTestCases ? testCase.getInput() : null)
                        .expectedOutput(includeAllTestCases ? testCase.getExpectedOutput() : null)
                        .weight(testCase.getWeight())
                        .timeoutSeconds(testCase.getTimeoutSeconds())
                        .sample(testCase.getSample())
                        .metadata(testCase.getMetadata())
                        .build())
                .collect(Collectors.toList());

        Submission latestSubmission = null;
        UUID currentUser = UserContext.getCurrentUserId();
        if (currentUser != null) {
            latestSubmission = submissionRepository.findTopByExercise_IdAndUserIdAndIsActiveTrueOrderByCreatedDesc(
                    exercise.getId(), currentUser).orElse(null);
        }

        Float bestScore = null;
        if (currentUser != null) {
            List<Submission> submissions = submissionRepository
                    .findByExercise_IdAndUserIdAndIsActiveTrueOrderByCreatedDesc(
                            exercise.getId(), currentUser);
            bestScore = submissions.stream()
                    .map(Submission::getGrade)
                    .filter(grade -> grade != null)
                    .max(Float::compareTo)
                    .orElse(null);
        }

        return ExerciseResponse.builder()
                .id(exercise.getId())
                .type(exercise.getType())
                .question(exercise.getQuestion())
                .options(exercise.getOptions())
                .testCases(testCaseDtos)
                .lastSubmissionStatus(latestSubmission != null ? latestSubmission.getStatus() : null)
                .bestScore(bestScore)
                .lastSubmittedAt(latestSubmission != null ? latestSubmission.getUpdated() : null)
                .build();
    }

    private Lesson resolveLesson(UUID courseId, UUID lessonId) {
        Lesson lesson = lessonRepository.findById(lessonId)
                .orElseThrow(() -> new NotFoundException("Lesson not found"));
        if (lesson.getChapter() == null || lesson.getChapter().getCourse() == null) {
            throw new NotFoundException("Lesson is not attached to a course");
        }
        if (!lesson.getChapter().getCourse().getId().equals(courseId)) {
            throw new ForbiddenException("Lesson does not belong to the specified course");
        }
        if (lesson.getIsActive() != null && !lesson.getIsActive()) {
            throw new NotFoundException("Lesson is not active");
        }
        return lesson;
    }

    private void ensureManagePermission(Course course) {
        if (!canManageCourse(course)) {
            throw new ForbiddenException("Only instructors or admins can manage exercises");
        }
    }

    private void ensureLearnerOrManager(Course course, UUID userId) {
        if (canManageCourse(course)) {
            return;
        }
        Enrollment enrollment = enrollmentRepository.findByUserIdAndCourse_IdAndIsActiveTrue(userId, course.getId())
                .orElse(null);
        if (enrollment == null) {
            throw new ForbiddenException("Only enrolled learners can submit exercises");
        }
    }

    private boolean canManageCourse(Course course) {
        UUID currentUserId = UserContext.getCurrentUserId();
        if (currentUserId == null) {
            return false;
        }
        if (UserContext.hasAnyRole("ADMIN")) {
            return true;
        }
        return UserContext.hasAnyRole("INSTRUCTOR") && course.getInstructorId() != null
                && course.getInstructorId().equals(currentUserId);
    }

    private UUID requireCurrentUser() {
        UUID userId = UserContext.getCurrentUserId();
        if (userId == null) {
            throw new UnauthorizedException("Authentication required");
        }
        return userId;
    }

    private void syncTestCases(Exercise exercise, List<ExerciseTestCaseDto> testCaseDtos) {
        List<ExerciseTestCase> existing = testCaseRepository
                .findByExercise_IdAndIsActiveTrueOrderByOrderIndexAsc(exercise.getId());
        Map<UUID, ExerciseTestCase> existingMap = existing.stream()
                .filter(tc -> tc.getId() != null)
                .collect(Collectors.toMap(ExerciseTestCase::getId, tc -> tc));

        Set<UUID> incomingIds = new HashSet<>();
        if (testCaseDtos != null) {
            for (ExerciseTestCaseDto dto : testCaseDtos) {
                if (dto == null) {
                    continue;
                }
                ExerciseTestCase entity;
                if (dto.getId() != null && existingMap.containsKey(dto.getId())) {
                    entity = existingMap.get(dto.getId());
                } else {
                    entity = new ExerciseTestCase();
                    entity.setExercise(exercise);
                    entity.setCreatedBy(UserContext.getCurrentUserId());
                    entity.setIsActive(true);
                }

                if (dto.getId() != null) {
                    incomingIds.add(dto.getId());
                }
                entity.setOrderIndex(dto.getOrderIndex() != null ? dto.getOrderIndex() : 0);
                entity.setVisibility(dto.getVisibility() != null ? dto.getVisibility() : TestCaseVisibility.PUBLIC);
                entity.setInput(dto.getInput());
                entity.setExpectedOutput(dto.getExpectedOutput());
                entity.setWeight(dto.getWeight() != null ? dto.getWeight() : 1f);
                entity.setTimeoutSeconds(dto.getTimeoutSeconds());
                entity.setSample(dto.getSample() != null ? dto.getSample() : Boolean.FALSE);
                entity.setMetadata(dto.getMetadata());
                entity.setUpdatedBy(UserContext.getCurrentUserId());
                testCaseRepository.save(entity);
            }
        }

        for (ExerciseTestCase existingCase : existing) {
            if (existingCase.getId() != null && (testCaseDtos == null || !incomingIds.contains(existingCase.getId()))) {
                existingCase.setIsActive(false);
                existingCase.setUpdatedBy(UserContext.getCurrentUserId());
                testCaseRepository.save(existingCase);
            }
        }
    }

    private static class ExerciseEvaluationResult {
        private final Float grade;
        private final SubmissionStatus status;
        private final List<TestCaseResultResponse> testCaseResults;

        ExerciseEvaluationResult(Float grade, SubmissionStatus status, List<TestCaseResultResponse> testCaseResults) {
            this.grade = grade;
            this.status = status;
            this.testCaseResults = testCaseResults;
        }

        public Float grade() {
            return grade;
        }

        public SubmissionStatus status() {
            return status;
        }

        public List<TestCaseResultResponse> testCaseResults() {
            return testCaseResults;
        }
    }
}
