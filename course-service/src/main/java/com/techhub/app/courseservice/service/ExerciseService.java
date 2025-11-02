package com.techhub.app.courseservice.service;

import com.techhub.app.courseservice.dto.request.ExerciseRequest;
import com.techhub.app.courseservice.dto.request.ExerciseSubmissionRequest;
import com.techhub.app.courseservice.dto.response.ExerciseResponse;
import com.techhub.app.courseservice.dto.response.ExerciseSubmissionResponse;

import java.util.UUID;

public interface ExerciseService {

    ExerciseResponse getLessonExercise(UUID courseId, UUID lessonId);

    ExerciseResponse upsertExercise(UUID courseId, UUID lessonId, ExerciseRequest request);

    ExerciseSubmissionResponse submitExercise(UUID courseId, UUID lessonId, ExerciseSubmissionRequest request);
}
