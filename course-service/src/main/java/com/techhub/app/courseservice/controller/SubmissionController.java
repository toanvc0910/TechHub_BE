package com.techhub.app.courseservice.controller;

import com.techhub.app.courseservice.dto.CreateSubmissionDTO;
import com.techhub.app.courseservice.dto.SubmissionDTO;
import com.techhub.app.courseservice.service.SubmissionService;
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
@RequestMapping("/submissions")
@Tag(name = "Submission Management", description = "APIs for managing exercise submissions")
public class SubmissionController {

    private final SubmissionService submissionService;

    public SubmissionController(SubmissionService submissionService) {
        this.submissionService = submissionService;
    }

    @PostMapping
    @Operation(summary = "Create a new submission")
    public ResponseEntity<ResponseWrapper<SubmissionDTO>> createSubmission(@Valid @RequestBody CreateSubmissionDTO createSubmissionDTO) {
        SubmissionDTO submissionDTO = submissionService.createSubmission(createSubmissionDTO);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ResponseWrapper.success(submissionDTO, "Submission created successfully"));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get submission by ID")
    public ResponseEntity<ResponseWrapper<SubmissionDTO>> getSubmissionById(@PathVariable UUID id) {
        SubmissionDTO submissionDTO = submissionService.getSubmissionById(id);
        return ResponseEntity.ok(ResponseWrapper.success(submissionDTO));
    }

    @GetMapping("/user/{userId}")
    @Operation(summary = "Get submissions by user")
    public ResponseEntity<ResponseWrapper<List<SubmissionDTO>>> getSubmissionsByUser(@PathVariable UUID userId) {
        List<SubmissionDTO> submissions = submissionService.getSubmissionsByUser(userId);
        return ResponseEntity.ok(ResponseWrapper.success(submissions));
    }

    @GetMapping("/exercise/{exerciseId}")
    @Operation(summary = "Get submissions by exercise")
    public ResponseEntity<ResponseWrapper<List<SubmissionDTO>>> getSubmissionsByExercise(@PathVariable UUID exerciseId) {
        List<SubmissionDTO> submissions = submissionService.getSubmissionsByExercise(exerciseId);
        return ResponseEntity.ok(ResponseWrapper.success(submissions));
    }

    @PatchMapping("/{id}/grade")
    @Operation(summary = "Grade a submission")
    public ResponseEntity<ResponseWrapper<SubmissionDTO>> gradeSubmission(
            @PathVariable UUID id,
            @RequestParam Double grade) {
        SubmissionDTO submissionDTO = submissionService.gradeSubmission(id, grade);
        return ResponseEntity.ok(ResponseWrapper.success(submissionDTO, "Submission graded successfully"));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete submission")
    public ResponseEntity<ResponseWrapper<Void>> deleteSubmission(@PathVariable UUID id) {
        submissionService.deleteSubmission(id);
        return ResponseEntity.ok(ResponseWrapper.success(null, "Submission deleted successfully"));
    }
}
