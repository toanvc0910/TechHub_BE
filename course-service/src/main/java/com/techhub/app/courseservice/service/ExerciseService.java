package com.techhub.app.courseservice.service;

import com.techhub.app.courseservice.dto.request.ExerciseRequest;
import com.techhub.app.courseservice.dto.request.ExerciseSubmissionRequest;
import com.techhub.app.courseservice.dto.response.ExerciseResponse;
import com.techhub.app.courseservice.dto.response.ExerciseSubmissionResponse;

import java.util.List;
import java.util.UUID;

public interface ExerciseService {

    ExerciseResponse getLessonExercise(UUID courseId, UUID lessonId);

    List<ExerciseResponse> getLessonExercises(UUID courseId, UUID lessonId);

    ExerciseResponse upsertExercise(UUID courseId, UUID lessonId, ExerciseRequest request);

    List<ExerciseResponse> createExercises(UUID courseId, UUID lessonId, List<ExerciseRequest> requests);

    ExerciseResponse updateExercise(UUID courseId, UUID lessonId, UUID exerciseId, ExerciseRequest request);

    void deleteExercise(UUID courseId, UUID lessonId, UUID exerciseId);

    ExerciseSubmissionResponse submitExercise(UUID courseId, UUID lessonId, ExerciseSubmissionRequest request);
}
