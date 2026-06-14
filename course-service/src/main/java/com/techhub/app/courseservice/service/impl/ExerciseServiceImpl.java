package com.techhub.app.courseservice.service.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.techhub.app.commonservice.context.UserContext;
import com.techhub.app.commonservice.enums.UserRole;
import com.techhub.app.commonservice.exception.BadRequestException;
import com.techhub.app.commonservice.exception.ForbiddenException;
import com.techhub.app.commonservice.exception.NotFoundException;
import com.techhub.app.commonservice.exception.UnauthorizedException;
import com.techhub.app.courseservice.client.AiExerciseFeedbackClient;
import com.techhub.app.courseservice.client.UserServiceClient;
import com.techhub.app.courseservice.dto.ExerciseTestCaseDto;
import com.techhub.app.courseservice.dto.request.ExerciseRequest;
import com.techhub.app.courseservice.dto.request.ExerciseSubmissionRequest;
import com.techhub.app.courseservice.dto.request.GradeSubmissionRequest;
import com.techhub.app.courseservice.dto.request.LessonProgressRequest;
import com.techhub.app.courseservice.dto.response.ExerciseResponse;
import com.techhub.app.courseservice.dto.response.ExerciseSubmissionResponse;
import com.techhub.app.courseservice.dto.response.QuizFeedbackResponse;
import com.techhub.app.courseservice.dto.response.ReviewSuggestionResponse;
import com.techhub.app.courseservice.dto.response.SubmissionResponse;
import com.techhub.app.courseservice.dto.response.TestCaseResultResponse;
import com.techhub.app.courseservice.entity.Course;
import com.techhub.app.courseservice.entity.Enrollment;
import com.techhub.app.courseservice.entity.Exercise;
import com.techhub.app.courseservice.entity.ExerciseTestCase;
import com.techhub.app.courseservice.entity.Lesson;
import com.techhub.app.courseservice.entity.Submission;
import com.techhub.app.courseservice.enums.CourseStatus;
import com.techhub.app.courseservice.enums.ExerciseType;
import com.techhub.app.courseservice.enums.SubmissionStatus;
import com.techhub.app.courseservice.enums.TestCaseVisibility;
import com.techhub.app.courseservice.repository.CourseRepository;
import com.techhub.app.courseservice.repository.EnrollmentRepository;
import com.techhub.app.courseservice.repository.ExerciseRepository;
import com.techhub.app.courseservice.repository.ExerciseTestCaseRepository;
import com.techhub.app.courseservice.repository.LessonRepository;
import com.techhub.app.courseservice.repository.SubmissionRepository;
import com.techhub.app.courseservice.service.CourseNotificationService;
import com.techhub.app.courseservice.service.CourseProgressService;
import com.techhub.app.courseservice.service.ExerciseService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
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
    private final CourseNotificationService courseNotificationService;
    private final ObjectMapper objectMapper;
    private final AiExerciseFeedbackClient aiExerciseFeedbackClient;
    private final UserServiceClient userServiceClient;

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
        Course course = lesson.getChapter().getCourse();
        ensureManagePermission(course);

        boolean isNewExercise = false;
        Exercise exercise = exerciseRepository.findByLesson_IdAndIsActiveTrue(lesson.getId())
                .orElseGet(() -> {
                    Exercise entity = new Exercise();
                    entity.setLesson(lesson);
                    entity.setOrderIndex(1);
                    entity.setCreatedBy(UserContext.getCurrentUserId());
                    entity.setIsActive(true);
                    return entity;
                });

        // Check if this is a new exercise (no ID yet)
        if (exercise.getId() == null) {
            isNewExercise = true;
        }

        exercise.setType(request.getType());
        exercise.setQuestion(request.getQuestion());
        exercise.setOptions(request.getOptions());
        exercise.setUpdatedBy(UserContext.getCurrentUserId());
        exerciseRepository.save(exercise);

        syncTestCases(exercise, request.getTestCases());

        // Send notification if new exercise was created on a published course
        if (isNewExercise && course.getStatus() == CourseStatus.PUBLISHED) {
            courseNotificationService.notifyNewExercise(
                    course.getId(),
                    course.getTitle(),
                    lesson.getId(),
                    lesson.getTitle());
        }

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
        Course course = lesson.getChapter().getCourse();
        ensureManagePermission(course);

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

        // Send notification to enrolled students about new exercises (only once, not
        // per exercise)
        if (!responses.isEmpty() && course.getStatus() == CourseStatus.PUBLISHED) {
            courseNotificationService.notifyNewExercise(
                    course.getId(),
                    course.getTitle(),
                    lesson.getId(),
                    lesson.getTitle());
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

        Exercise exercise = resolveSubmissionExercise(lesson, request.getExerciseId());

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
        QuizFeedbackResponse feedback = buildQuizFeedback(lesson, exercise, request, evaluation);

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
                .feedback(feedback)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public List<SubmissionResponse> getExerciseSubmissions(UUID courseId, UUID lessonId, UUID exerciseId) {
        Lesson lesson = resolveLesson(courseId, lessonId);
        ensureManagePermission(lesson.getChapter().getCourse());

        Exercise exercise = exerciseRepository.findById(exerciseId)
                .orElseThrow(() -> new NotFoundException("Exercise not found"));
        if (!exercise.getLesson().getId().equals(lessonId)) {
            throw new ForbiddenException("Exercise does not belong to the specified lesson");
        }

        List<Submission> submissions = submissionRepository
                .findByExercise_IdAndIsActiveTrueOrderByCreatedDesc(exerciseId);

        // Submissions arrive newest-first, so the first one seen per learner is
        // their latest attempt — the one an instructor wants to review and grade.
        Map<UUID, Submission> latestByUser = new LinkedHashMap<>();
        for (Submission submission : submissions) {
            latestByUser.putIfAbsent(submission.getUserId(), submission);
        }

        Map<UUID, Map<String, Object>> userInfo = fetchUserInfo(new ArrayList<>(latestByUser.keySet()));

        return latestByUser.values().stream()
                .map(submission -> toSubmissionResponse(submission,
                        userInfo.getOrDefault(submission.getUserId(), Collections.emptyMap())))
                .collect(Collectors.toList());
    }

    @Override
    public SubmissionResponse gradeSubmission(UUID courseId, UUID lessonId, UUID submissionId,
            GradeSubmissionRequest request) {
        Lesson lesson = resolveLesson(courseId, lessonId);
        ensureManagePermission(lesson.getChapter().getCourse());

        Submission submission = submissionRepository.findById(submissionId)
                .orElseThrow(() -> new NotFoundException("Submission not found"));
        if (submission.getExercise() == null || submission.getExercise().getLesson() == null
                || !lessonId.equals(submission.getExercise().getLesson().getId())) {
            throw new ForbiddenException("Submission does not belong to the specified lesson");
        }
        if (request.getGrade() != null && (request.getGrade() < 0f || request.getGrade() > 100f)) {
            throw new BadRequestException("Grade must be between 0 and 100");
        }

        UUID graderId = requireCurrentUser();
        submission.setGrade(request.getGrade());
        submission.setFeedback(request.getFeedback());
        submission.setGradedBy(graderId);
        submission.setGradedAt(OffsetDateTime.now());
        submission.setUpdatedBy(graderId);

        if (request.getStatus() != null) {
            submission.setStatus(request.getStatus());
        } else if (request.getGrade() != null) {
            // Derive a pass/fail from the score so the entry leaves PENDING and
            // starts counting toward the lesson leaderboard.
            submission.setStatus(
                    request.getGrade() >= 50f ? SubmissionStatus.PASSED : SubmissionStatus.FAILED);
        }
        // Feedback-only grading (no score, no explicit status) keeps the current status.

        submissionRepository.save(submission);
        log.info("Submission {} graded by {} (grade={}, status={})",
                submission.getId(), graderId, submission.getGrade(), submission.getStatus());

        Map<UUID, Map<String, Object>> userInfo = fetchUserInfo(List.of(submission.getUserId()));
        return toSubmissionResponse(submission,
                userInfo.getOrDefault(submission.getUserId(), Collections.emptyMap()));
    }

    private SubmissionResponse toSubmissionResponse(Submission submission, Map<String, Object> user) {
        Object username = user.get("username");
        Object avatar = user.get("avatar");
        return SubmissionResponse.builder()
                .id(submission.getId())
                .userId(submission.getUserId())
                .username(username != null ? String.valueOf(username) : null)
                .avatar(avatar != null ? String.valueOf(avatar) : null)
                .answer(submission.getAnswer())
                .submissionData(submission.getSubmissionData())
                .grade(submission.getGrade())
                .feedback(submission.getFeedback())
                .status(submission.getStatus())
                .submittedAt(submission.getCreated())
                .gradedAt(submission.getGradedAt())
                .gradedBy(submission.getGradedBy())
                .build();
    }

    @SuppressWarnings("unchecked")
    private Map<UUID, Map<String, Object>> fetchUserInfo(List<UUID> ids) {
        if (ids == null || ids.isEmpty()) {
            return Collections.emptyMap();
        }
        try {
            UUID currentUserId = UserContext.getCurrentUserId();
            Map<String, Object> resp = userServiceClient.getUsersBatch(
                    ids,
                    currentUserId != null ? currentUserId.toString() : null,
                    UserContext.getCurrentUserEmail(),
                    currentUserRoles(),
                    "course-service");
            Object data = resp == null ? null : resp.get("data");
            if (!(data instanceof List)) {
                return Collections.emptyMap();
            }
            Map<UUID, Map<String, Object>> map = new HashMap<>();
            for (Object item : (List<Object>) data) {
                if (!(item instanceof Map)) {
                    continue;
                }
                Map<String, Object> entry = (Map<String, Object>) item;
                Object idObj = entry.get("id");
                if (idObj == null) {
                    continue;
                }
                UUID id = idObj instanceof UUID ? (UUID) idObj : UUID.fromString(idObj.toString());
                map.put(id, entry);
            }
            return map;
        } catch (Exception ex) {
            log.warn("[Submissions] Failed fetching user info batch: {}", ex.getMessage());
            return Collections.emptyMap();
        }
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
            List<Map<String, Object>> choices = extractChoices(exercise);

            List<String> answers = objectMapper.readValue(request.getAnswer(), new TypeReference<List<String>>() {
            });
            Set<String> submitted = new HashSet<>(answers);

            int correctCount = 0;
            int selectedCorrectCount = 0;
            int selectedIncorrectCount = 0;

            for (int index = 0; index < choices.size(); index++) {
                Map<String, Object> choice = choices.get(index);
                boolean correctChoice = isChoiceCorrect(choice);
                boolean selected = isChoiceSelected(choice, index, submitted);
                if (correctChoice) {
                    correctCount++;
                }
                if (selected && correctChoice) {
                    selectedCorrectCount++;
                }
                if (selected && !correctChoice) {
                    selectedIncorrectCount++;
                }
            }

            boolean passed = correctCount > 0
                    && selectedCorrectCount == correctCount
                    && selectedIncorrectCount == 0
                    && countSelectedChoices(choices, submitted) == correctCount;
            float grade = passed ? 100f : 0f;
            List<String> correctAnswers = collectChoiceTexts(choices, submitted, true, false);
            List<String> selectedAnswers = collectChoiceTexts(choices, submitted, false, true);

            TestCaseResultResponse result = TestCaseResultResponse.builder()
                    .testCaseId(null)
                    .passed(passed)
                    .input(null)
                    .expectedOutput(String.join(",", correctAnswers))
                    .actualOutput(String.join(",", selectedAnswers))
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

    private QuizFeedbackResponse buildQuizFeedback(
            Lesson lesson,
            Exercise exercise,
            ExerciseSubmissionRequest request,
            ExerciseEvaluationResult evaluation) {
        if (exercise.getType() != ExerciseType.MULTIPLE_CHOICE) {
            return null;
        }

        List<Map<String, Object>> choices = extractChoices(exercise);
        Set<String> submitted = parseSubmittedAnswers(request);
        List<String> selectedAnswers = collectChoiceTexts(choices, submitted, false, true);
        List<String> correctAnswers = collectChoiceTexts(choices, submitted, true, false);
        boolean correct = evaluation.status() == SubmissionStatus.PASSED;
        String baseExplanation = extractExerciseExplanation(exercise);

        QuizFeedbackResponse fallback = buildFallbackQuizFeedback(
                lesson,
                exercise,
                correct,
                selectedAnswers,
                correctAnswers,
                baseExplanation);

        if (correct) {
            return fallback;
        }

        try {
            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("courseId", String.valueOf(lesson.getChapter().getCourse().getId()));
            payload.put("courseTitle", lesson.getChapter().getCourse().getTitle());
            payload.put("lessonId", String.valueOf(lesson.getId()));
            payload.put("lessonTitle", lesson.getTitle());
            payload.put("question", exercise.getQuestion());
            payload.put("options", choices);
            payload.put("selectedAnswers", selectedAnswers);
            payload.put("correctAnswers", correctAnswers);
            payload.put("isCorrect", correct);
            payload.put("explanation", baseExplanation);
            payload.put("language", "vi");

            String body = aiExerciseFeedbackClient.generateQuizFeedback(
                    payload,
                    String.valueOf(requireCurrentUser()),
                    UserContext.getCurrentUserEmail(),
                    currentUserRoles(),
                    "course-service").getBody();
            if (body == null || body.isBlank()) {
                return fallback;
            }

            JsonNode root = objectMapper.readTree(body);
            JsonNode data = root.has("data") ? root.get("data") : root;
            QuizFeedbackResponse aiFeedback = objectMapper.convertValue(data, QuizFeedbackResponse.class);
            if (aiFeedback == null) {
                return fallback;
            }

            if (aiFeedback.getCorrect() == null) {
                aiFeedback.setCorrect(correct);
            }
            if (aiFeedback.getSelectedAnswers() == null || aiFeedback.getSelectedAnswers().isEmpty()) {
                aiFeedback.setSelectedAnswers(selectedAnswers);
            }
            if (aiFeedback.getCorrectAnswers() == null || aiFeedback.getCorrectAnswers().isEmpty()) {
                aiFeedback.setCorrectAnswers(correctAnswers);
            }
            if (aiFeedback.getReviewSuggestions() == null || aiFeedback.getReviewSuggestions().isEmpty()) {
                aiFeedback.setReviewSuggestions(fallback.getReviewSuggestions());
            }
            if (aiFeedback.getSource() == null || aiFeedback.getSource().isBlank()) {
                aiFeedback.setSource("AI_SERVICE");
            }
            return aiFeedback;
        } catch (Exception ex) {
            log.warn("AI quiz feedback unavailable for exercise {}: {}", exercise.getId(), ex.getMessage());
            return fallback;
        }
    }

    private QuizFeedbackResponse buildFallbackQuizFeedback(
            Lesson lesson,
            Exercise exercise,
            boolean correct,
            List<String> selectedAnswers,
            List<String> correctAnswers,
            String baseExplanation) {
        String lessonTitle = lesson.getTitle() != null ? lesson.getTitle() : "bai hoc nay";
        String selectedText = selectedAnswers.isEmpty() ? "chua chon dap an" : String.join(", ", selectedAnswers);
        String correctText = correctAnswers.isEmpty() ? "dap an dung" : String.join(", ", correctAnswers);

        return QuizFeedbackResponse.builder()
                .correct(correct)
                .summary(correct
                        ? "Ban da nam dung y chinh cua cau hoi nay."
                        : "Cau tra loi cua ban chua khop voi trong tam kien thuc cua bai hoc.")
                .explanation(correct
                        ? "Lua chon cua ban phu hop voi noi dung bai hoc."
                        : buildFallbackExplanation(baseExplanation, selectedText, correctText))
                .selectedAnswers(selectedAnswers)
                .correctAnswers(correctAnswers)
                .weakConcepts(extractWeakConcepts(exercise.getQuestion()))
                .reviewSuggestions(List.of(ReviewSuggestionResponse.builder()
                        .lessonId(lesson.getId())
                        .title(lessonTitle)
                        .reason(correct
                                ? "Tiep tuc hoc bai tiep theo de giu mach kien thuc."
                                : "On lai phan noi dung lien quan truc tiep den cau hoi vua sai.")
                        .action(correct
                                ? "Chuyen sang cau tiep theo hoac bai tiep theo."
                                : "Doc lai noi dung bai, sau do lam lai cau hoi nay.")
                        .build()))
                .nextAction(correct
                        ? "Tiep tuc voi cau hoi tiep theo."
                        : "On lai bai '" + lessonTitle + "' truoc khi lam lai.")
                .source("COURSE_SERVICE_FALLBACK")
                .build();
    }

    private String buildFallbackExplanation(String baseExplanation, String selectedText, String correctText) {
        if (baseExplanation != null && !baseExplanation.isBlank()) {
            return baseExplanation + " Dap an ban chon: " + selectedText + ". Dap an dung: " + correctText + ".";
        }
        return "Dap an ban chon la " + selectedText
                + ", trong khi dap an dung la " + correctText
                + ". Hay xem lai noi dung bai hoc lien quan den cau hoi nay.";
    }

    private Exercise resolveSubmissionExercise(Lesson lesson, UUID exerciseId) {
        if (exerciseId == null) {
            return exerciseRepository.findByLesson_IdAndIsActiveTrue(lesson.getId())
                    .orElseThrow(() -> new NotFoundException("Exercise not found for lesson"));
        }

        Exercise exercise = exerciseRepository.findById(exerciseId)
                .orElseThrow(() -> new NotFoundException("Exercise not found"));
        if (!lesson.getId().equals(exercise.getLesson().getId())) {
            throw new ForbiddenException("Exercise does not belong to the specified lesson");
        }
        if (exercise.getIsActive() != null && !exercise.getIsActive()) {
            throw new NotFoundException("Exercise is not active");
        }
        return exercise;
    }

    private List<Map<String, Object>> extractChoices(Exercise exercise) {
        Map<String, Object> optionPayload = parseOptions(exercise.getOptions());
        Object rawChoices = optionPayload.getOrDefault("choices", List.of());
        if (!(rawChoices instanceof List<?>)) {
            return List.of();
        }

        List<Map<String, Object>> choices = new ArrayList<>();
        for (Object rawChoice : (List<?>) rawChoices) {
            if (rawChoice instanceof Map<?, ?>) {
                choices.add(objectMapper.convertValue(rawChoice, new TypeReference<Map<String, Object>>() {
                }));
            }
        }
        return choices;
    }

    private Map<String, Object> parseOptions(Object rawOptions) {
        if (rawOptions == null) {
            return Collections.emptyMap();
        }
        try {
            if (rawOptions instanceof String) {
                return objectMapper.readValue((String) rawOptions, new TypeReference<Map<String, Object>>() {
                });
            }
            return objectMapper.convertValue(rawOptions, new TypeReference<Map<String, Object>>() {
            });
        } catch (Exception ex) {
            log.warn("Invalid exercise options payload: {}", ex.getMessage());
            return Collections.emptyMap();
        }
    }

    private Set<String> parseSubmittedAnswers(ExerciseSubmissionRequest request) {
        try {
            List<String> answers = objectMapper.readValue(request.getAnswer(), new TypeReference<List<String>>() {
            });
            return new HashSet<>(answers);
        } catch (Exception ex) {
            if (request.getAnswer() == null || request.getAnswer().isBlank()) {
                return Set.of();
            }
            return Set.of(request.getAnswer());
        }
    }

    private boolean isChoiceCorrect(Map<String, Object> choice) {
        return Boolean.TRUE.equals(choice.get("correct")) || Boolean.TRUE.equals(choice.get("isCorrect"));
    }

    private boolean isChoiceSelected(Map<String, Object> choice, int index, Set<String> submitted) {
        String id = choiceId(choice, index);
        String text = choiceText(choice);
        return submitted.contains(id) || submitted.contains(String.valueOf(index)) || submitted.contains(text);
    }

    private String choiceId(Map<String, Object> choice, int index) {
        Object rawId = choice.get("id");
        if (rawId == null || String.valueOf(rawId).isBlank()) {
            return String.valueOf(index);
        }
        return String.valueOf(rawId);
    }

    private String choiceText(Map<String, Object> choice) {
        Object rawText = choice.get("text");
        return rawText == null ? "" : String.valueOf(rawText);
    }

    private int countSelectedChoices(List<Map<String, Object>> choices, Set<String> submitted) {
        int selected = 0;
        for (int index = 0; index < choices.size(); index++) {
            if (isChoiceSelected(choices.get(index), index, submitted)) {
                selected++;
            }
        }
        return selected;
    }

    private List<String> collectChoiceTexts(
            List<Map<String, Object>> choices,
            Set<String> submitted,
            boolean correctOnly,
            boolean selectedOnly) {
        List<String> labels = new ArrayList<>();
        for (int index = 0; index < choices.size(); index++) {
            Map<String, Object> choice = choices.get(index);
            if (correctOnly && !isChoiceCorrect(choice)) {
                continue;
            }
            if (selectedOnly && !isChoiceSelected(choice, index, submitted)) {
                continue;
            }
            String text = choiceText(choice);
            if (!text.isBlank()) {
                labels.add(text);
            }
        }
        return labels;
    }

    private String extractExerciseExplanation(Exercise exercise) {
        Map<String, Object> options = parseOptions(exercise.getOptions());
        Object explanation = options.get("explanation");
        return explanation == null ? null : String.valueOf(explanation);
    }

    private List<String> extractWeakConcepts(String question) {
        if (question == null || question.isBlank()) {
            return List.of("noi dung bai hoc");
        }
        String normalized = question.replaceAll("<[^>]+>", " ");
        String[] tokens = normalized.split("[^\\p{L}\\p{N}_]+");
        List<String> concepts = new ArrayList<>();
        for (String token : tokens) {
            if (token.length() >= 5 && concepts.size() < 3) {
                concepts.add(token);
            }
        }
        return concepts.isEmpty() ? List.of("noi dung bai hoc") : concepts;
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
                .lastAnswer(latestSubmission != null ? latestSubmission.getAnswer() : null)
                .lastFeedback(latestSubmission != null ? latestSubmission.getFeedback() : null)
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
        if (UserContext.hasAnyRole(UserRole.ADMIN.name(), UserRole.SUPER_ADMIN.name())) {
            return true;
        }
        return UserContext.hasAnyRole(UserRole.INSTRUCTOR.name()) && course.getInstructorId() != null
                && course.getInstructorId().equals(currentUserId);
    }

    private UUID requireCurrentUser() {
        UUID userId = UserContext.getCurrentUserId();
        if (userId == null) {
            throw new UnauthorizedException("Authentication required");
        }
        return userId;
    }

    private String currentUserRoles() {
        List<String> roles = UserContext.getCurrentUserRoles();
        return roles == null ? "" : String.join(",", roles);
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
