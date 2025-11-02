package com.techhub.app.courseservice.controller;

import com.techhub.app.courseservice.dto.CreateProgressDTO;
import com.techhub.app.courseservice.dto.ProgressDTO;
import com.techhub.app.courseservice.service.ProgressService;
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
@RequestMapping("/progress")
@Tag(name = "Progress Tracking", description = "APIs for tracking user progress")
public class ProgressController {

    private final ProgressService progressService;

    public ProgressController(ProgressService progressService) {
        this.progressService = progressService;
    }

    @PostMapping
    @Operation(summary = "Create or update progress")
    public ResponseEntity<ResponseWrapper<ProgressDTO>> createProgress(@Valid @RequestBody CreateProgressDTO createProgressDTO) {
        ProgressDTO progressDTO = progressService.createProgress(createProgressDTO);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ResponseWrapper.success(progressDTO, "Progress saved successfully"));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get progress by ID")
    public ResponseEntity<ResponseWrapper<ProgressDTO>> getProgressById(@PathVariable UUID id) {
        ProgressDTO progressDTO = progressService.getProgressById(id);
        return ResponseEntity.ok(ResponseWrapper.success(progressDTO));
    }

    @GetMapping("/user/{userId}")
    @Operation(summary = "Get progress by user")
    public ResponseEntity<ResponseWrapper<List<ProgressDTO>>> getProgressByUser(@PathVariable UUID userId) {
        List<ProgressDTO> progress = progressService.getProgressByUser(userId);
        return ResponseEntity.ok(ResponseWrapper.success(progress));
    }

    @GetMapping("/user/{userId}/lesson/{lessonId}")
    @Operation(summary = "Get progress by user and lesson")
    public ResponseEntity<ResponseWrapper<ProgressDTO>> getProgressByUserAndLesson(
            @PathVariable UUID userId,
            @PathVariable UUID lessonId) {
        ProgressDTO progressDTO = progressService.getProgressByUserAndLesson(userId, lessonId);
        return ResponseEntity.ok(ResponseWrapper.success(progressDTO));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update progress")
    public ResponseEntity<ResponseWrapper<ProgressDTO>> updateProgress(
            @PathVariable UUID id,
            @RequestBody CreateProgressDTO updateProgressDTO) {
        ProgressDTO progressDTO = progressService.updateProgress(id, updateProgressDTO);
        return ResponseEntity.ok(ResponseWrapper.success(progressDTO, "Progress updated successfully"));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete progress")
    public ResponseEntity<ResponseWrapper<Void>> deleteProgress(@PathVariable UUID id) {
        progressService.deleteProgress(id);
        return ResponseEntity.ok(ResponseWrapper.success(null, "Progress deleted successfully"));
    }
}
