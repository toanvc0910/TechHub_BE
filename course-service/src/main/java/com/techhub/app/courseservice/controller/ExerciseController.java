package com.techhub.app.courseservice.controller;

import com.techhub.app.courseservice.dto.CreateExerciseDTO;
import com.techhub.app.courseservice.dto.ExerciseDTO;
import com.techhub.app.courseservice.service.ExerciseService;
import com.techhub.app.courseservice.utils.ResponseWrapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import javax.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/exercises")
@Tag(name = "Exercise Management", description = "APIs for managing exercises")
public class ExerciseController {

    private final ExerciseService exerciseService;

    public ExerciseController(ExerciseService exerciseService) {
        this.exerciseService = exerciseService;
    }

    @PostMapping
    @Operation(summary = "Create a new exercise")
    public ResponseEntity<ResponseWrapper<ExerciseDTO>> createExercise(@Valid @RequestBody CreateExerciseDTO createExerciseDTO) {
        ExerciseDTO exerciseDTO = exerciseService.createExercise(createExerciseDTO);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ResponseWrapper.success(exerciseDTO, "Exercise created successfully"));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get exercise by ID")
    public ResponseEntity<ResponseWrapper<ExerciseDTO>> getExerciseById(@PathVariable UUID id) {
        ExerciseDTO exerciseDTO = exerciseService.getExerciseById(id);
        return ResponseEntity.ok(ResponseWrapper.success(exerciseDTO));
    }

    @GetMapping
    @Operation(summary = "Get all exercises")
    public ResponseEntity<ResponseWrapper<List<ExerciseDTO>>> getAllExercises() {
        List<ExerciseDTO> exercises = exerciseService.getAllExercises();
        return ResponseEntity.ok(ResponseWrapper.success(exercises));
    }

    @GetMapping("/lesson/{lessonId}")
    @Operation(summary = "Get exercises by lesson")
    public ResponseEntity<ResponseWrapper<List<ExerciseDTO>>> getExercisesByLesson(@PathVariable UUID lessonId) {
        List<ExerciseDTO> exercises = exerciseService.getExercisesByLesson(lessonId);
        return ResponseEntity.ok(ResponseWrapper.success(exercises));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update exercise")
    public ResponseEntity<ResponseWrapper<ExerciseDTO>> updateExercise(
            @PathVariable UUID id,
            @RequestBody CreateExerciseDTO updateExerciseDTO) {
        ExerciseDTO exerciseDTO = exerciseService.updateExercise(id, updateExerciseDTO);
        return ResponseEntity.ok(ResponseWrapper.success(exerciseDTO, "Exercise updated successfully"));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete exercise")
    public ResponseEntity<ResponseWrapper<Void>> deleteExercise(@PathVariable UUID id) {
        exerciseService.deleteExercise(id);
        return ResponseEntity.ok(ResponseWrapper.success(null, "Exercise deleted successfully"));
    }
}
