package com.techhub.app.courseservice.controller;

import com.techhub.app.commonservice.payload.GlobalResponse;
import com.techhub.app.courseservice.dto.request.ExerciseRequest;
import com.techhub.app.courseservice.dto.request.ExerciseSubmissionRequest;
import com.techhub.app.courseservice.dto.response.ExerciseResponse;
import com.techhub.app.courseservice.dto.response.ExerciseSubmissionResponse;
import com.techhub.app.courseservice.service.ExerciseService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import java.util.UUID;

@RestController
@RequestMapping("/api/courses/{courseId}/lessons/{lessonId}/exercise")
@RequiredArgsConstructor
@Slf4j
@Validated
public class ExerciseController {

    private final ExerciseService exerciseService;

    @GetMapping
    public ResponseEntity<GlobalResponse<ExerciseResponse>> getLessonExercise(@PathVariable UUID courseId,
                                                                              @PathVariable UUID lessonId,
                                                                              HttpServletRequest request) {
        ExerciseResponse response = exerciseService.getLessonExercise(courseId, lessonId);
        return ResponseEntity.ok(
                GlobalResponse.success("Exercise retrieved", response)
                        .withPath(request.getRequestURI())
        );
    }

    @PutMapping
    public ResponseEntity<GlobalResponse<ExerciseResponse>> upsertExercise(@PathVariable UUID courseId,
                                                                           @PathVariable UUID lessonId,
                                                                           @Valid @RequestBody ExerciseRequest exerciseRequest,
                                                                           HttpServletRequest request) {
        ExerciseResponse response = exerciseService.upsertExercise(courseId, lessonId, exerciseRequest);
        return ResponseEntity.ok(
                GlobalResponse.success("Exercise saved", response)
                        .withStatus("EXERCISE_SAVED")
                        .withPath(request.getRequestURI())
        );
    }

    @PostMapping("/submissions")
    public ResponseEntity<GlobalResponse<ExerciseSubmissionResponse>> submitExercise(@PathVariable UUID courseId,
                                                                                      @PathVariable UUID lessonId,
                                                                                      @Valid @RequestBody ExerciseSubmissionRequest submissionRequest,
                                                                                      HttpServletRequest request) {
        ExerciseSubmissionResponse response = exerciseService.submitExercise(courseId, lessonId, submissionRequest);
        return ResponseEntity.ok(
                GlobalResponse.success("Exercise submitted", response)
                        .withStatus("EXERCISE_SUBMITTED")
                        .withPath(request.getRequestURI())
        );
    }
}
