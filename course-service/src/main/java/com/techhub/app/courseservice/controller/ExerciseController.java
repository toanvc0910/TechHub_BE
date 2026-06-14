package com.techhub.app.courseservice.controller;

import com.techhub.app.commonservice.payload.GlobalResponse;
import com.techhub.app.courseservice.dto.request.ExerciseRequest;
import com.techhub.app.courseservice.dto.request.ExerciseSubmissionRequest;
import com.techhub.app.courseservice.dto.request.GradeSubmissionRequest;
import com.techhub.app.courseservice.dto.response.ExerciseResponse;
import com.techhub.app.courseservice.dto.response.ExerciseSubmissionResponse;
import com.techhub.app.courseservice.dto.response.LeaderboardEntryResponse;
import com.techhub.app.courseservice.dto.response.SubmissionResponse;
import com.techhub.app.courseservice.service.ExerciseService;
import com.techhub.app.courseservice.service.LeaderboardService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/courses/{courseId}/lessons/{lessonId}")
@RequiredArgsConstructor
@Slf4j
@Validated
public class ExerciseController {

        private final ExerciseService exerciseService;
        private final LeaderboardService leaderboardService;

        // Legacy single exercise endpoint
        @GetMapping("/exercise")
        public ResponseEntity<GlobalResponse<ExerciseResponse>> getLessonExercise(@PathVariable UUID courseId,
                        @PathVariable UUID lessonId,
                        HttpServletRequest request) {
                ExerciseResponse response = exerciseService.getLessonExercise(courseId, lessonId);
                return ResponseEntity.ok(
                                GlobalResponse.success("Exercise retrieved", response)
                                                .withPath(request.getRequestURI()));
        }

        // New multiple exercises endpoint
        @GetMapping("/exercises")
        public ResponseEntity<GlobalResponse<List<ExerciseResponse>>> getLessonExercises(@PathVariable UUID courseId,
                        @PathVariable UUID lessonId,
                        HttpServletRequest request) {
                List<ExerciseResponse> responses = exerciseService.getLessonExercises(courseId, lessonId);
                return ResponseEntity.ok(
                                GlobalResponse.success("Exercises retrieved", responses)
                                                .withPath(request.getRequestURI()));
        }

        @PutMapping("/exercise")
        public ResponseEntity<GlobalResponse<ExerciseResponse>> upsertExercise(@PathVariable UUID courseId,
                        @PathVariable UUID lessonId,
                        @Valid @RequestBody ExerciseRequest exerciseRequest,
                        HttpServletRequest request) {
                ExerciseResponse response = exerciseService.upsertExercise(courseId, lessonId, exerciseRequest);
                return ResponseEntity.ok(
                                GlobalResponse.success("Exercise saved", response)
                                                .withStatus("EXERCISE_SAVED")
                                                .withPath(request.getRequestURI()));
        }

        // New endpoint to create multiple exercises at once
        @PostMapping("/exercises")
        public ResponseEntity<GlobalResponse<List<ExerciseResponse>>> createExercises(@PathVariable UUID courseId,
                        @PathVariable UUID lessonId,
                        @Valid @RequestBody List<ExerciseRequest> exerciseRequests,
                        HttpServletRequest request) {
                List<ExerciseResponse> responses = exerciseService.createExercises(courseId, lessonId,
                                exerciseRequests);
                return ResponseEntity.ok(
                                GlobalResponse.success("Exercises created", responses)
                                                .withStatus("EXERCISES_CREATED")
                                                .withPath(request.getRequestURI()));
        }

        // Update specific exercise
        @PutMapping("/exercises/{exerciseId}")
        public ResponseEntity<GlobalResponse<ExerciseResponse>> updateExercise(@PathVariable UUID courseId,
                        @PathVariable UUID lessonId,
                        @PathVariable UUID exerciseId,
                        @Valid @RequestBody ExerciseRequest exerciseRequest,
                        HttpServletRequest request) {
                ExerciseResponse response = exerciseService.updateExercise(courseId, lessonId, exerciseId,
                                exerciseRequest);
                return ResponseEntity.ok(
                                GlobalResponse.success("Exercise updated", response)
                                                .withStatus("EXERCISE_UPDATED")
                                                .withPath(request.getRequestURI()));
        }

        // Delete specific exercise
        @DeleteMapping("/exercises/{exerciseId}")
        public ResponseEntity<GlobalResponse<Void>> deleteExercise(@PathVariable UUID courseId,
                        @PathVariable UUID lessonId,
                        @PathVariable UUID exerciseId,
                        HttpServletRequest request) {
                exerciseService.deleteExercise(courseId, lessonId, exerciseId);
                return ResponseEntity.ok(
                                GlobalResponse.<Void>success("Exercise deleted", null)
                                                .withStatus("EXERCISE_DELETED")
                                                .withPath(request.getRequestURI()));
        }

        @PostMapping("/exercise/submissions")
        public ResponseEntity<GlobalResponse<ExerciseSubmissionResponse>> submitExercise(@PathVariable UUID courseId,
                        @PathVariable UUID lessonId,
                        @Valid @RequestBody ExerciseSubmissionRequest submissionRequest,
                        HttpServletRequest request) {
                ExerciseSubmissionResponse response = exerciseService.submitExercise(courseId, lessonId,
                                submissionRequest);
                return ResponseEntity.ok(
                                GlobalResponse.success("Exercise submitted", response)
                                                .withStatus("EXERCISE_SUBMITTED")
                                                .withPath(request.getRequestURI()));
        }

        // List the latest submission per learner for an exercise (instructor/admin)
        @GetMapping("/exercises/{exerciseId}/submissions")
        public ResponseEntity<GlobalResponse<List<SubmissionResponse>>> getExerciseSubmissions(
                        @PathVariable UUID courseId,
                        @PathVariable UUID lessonId,
                        @PathVariable UUID exerciseId,
                        HttpServletRequest request) {
                List<SubmissionResponse> responses = exerciseService.getExerciseSubmissions(courseId, lessonId,
                                exerciseId);
                return ResponseEntity.ok(
                                GlobalResponse.success("Submissions retrieved", responses)
                                                .withPath(request.getRequestURI()));
        }

        // Grade a submission with a score and/or written feedback (instructor/admin)
        @PutMapping("/submissions/{submissionId}/grade")
        public ResponseEntity<GlobalResponse<SubmissionResponse>> gradeSubmission(
                        @PathVariable UUID courseId,
                        @PathVariable UUID lessonId,
                        @PathVariable UUID submissionId,
                        @Valid @RequestBody GradeSubmissionRequest gradeRequest,
                        HttpServletRequest request) {
                SubmissionResponse response = exerciseService.gradeSubmission(courseId, lessonId, submissionId,
                                gradeRequest);
                return ResponseEntity.ok(
                                GlobalResponse.success("Submission graded", response)
                                                .withStatus("SUBMISSION_GRADED")
                                                .withPath(request.getRequestURI()));
        }

        @GetMapping("/leaderboard")
        public ResponseEntity<GlobalResponse<List<LeaderboardEntryResponse>>> getLessonLeaderboard(
                        @PathVariable UUID courseId,
                        @PathVariable UUID lessonId,
                        @org.springframework.web.bind.annotation.RequestParam(value = "limit", defaultValue = "10") int limit,
                        HttpServletRequest request) {
                List<LeaderboardEntryResponse> result = leaderboardService.getLessonLeaderboard(courseId, lessonId, limit);
                return ResponseEntity.ok(
                                GlobalResponse.success("Leaderboard retrieved", result)
                                                .withPath(request.getRequestURI()));
        }
}
