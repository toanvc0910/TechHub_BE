package com.techhub.app.courseservice.controller;

import com.techhub.app.courseservice.dto.CreateEnrollmentDTO;
import com.techhub.app.courseservice.dto.EnrollmentDTO;
import com.techhub.app.courseservice.service.EnrollmentService;
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
@RequestMapping("/enrollments")
@Tag(name = "Enrollment Management", description = "APIs for managing course enrollments")
public class EnrollmentController {

    private final EnrollmentService enrollmentService;

    public EnrollmentController(EnrollmentService enrollmentService) {
        this.enrollmentService = enrollmentService;
    }

    @PostMapping
    @Operation(summary = "Create a new enrollment")
    public ResponseEntity<ResponseWrapper<EnrollmentDTO>> createEnrollment(@Valid @RequestBody CreateEnrollmentDTO createEnrollmentDTO) {
        EnrollmentDTO enrollmentDTO = enrollmentService.createEnrollment(createEnrollmentDTO);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ResponseWrapper.success(enrollmentDTO, "Enrollment created successfully"));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get enrollment by ID")
    public ResponseEntity<ResponseWrapper<EnrollmentDTO>> getEnrollmentById(@PathVariable UUID id) {
        EnrollmentDTO enrollmentDTO = enrollmentService.getEnrollmentById(id);
        return ResponseEntity.ok(ResponseWrapper.success(enrollmentDTO));
    }

    @GetMapping("/user/{userId}")
    @Operation(summary = "Get enrollments by user")
    public ResponseEntity<ResponseWrapper<List<EnrollmentDTO>>> getEnrollmentsByUser(@PathVariable UUID userId) {
        List<EnrollmentDTO> enrollments = enrollmentService.getEnrollmentsByUser(userId);
        return ResponseEntity.ok(ResponseWrapper.success(enrollments));
    }

    @GetMapping("/course/{courseId}")
    @Operation(summary = "Get enrollments by course")
    public ResponseEntity<ResponseWrapper<List<EnrollmentDTO>>> getEnrollmentsByCourse(@PathVariable UUID courseId) {
        List<EnrollmentDTO> enrollments = enrollmentService.getEnrollmentsByCourse(courseId);
        return ResponseEntity.ok(ResponseWrapper.success(enrollments));
    }

    @PatchMapping("/{id}/status")
    @Operation(summary = "Update enrollment status")
    public ResponseEntity<ResponseWrapper<EnrollmentDTO>> updateEnrollmentStatus(
            @PathVariable UUID id,
            @RequestParam String status) {
        EnrollmentDTO enrollmentDTO = enrollmentService.updateEnrollmentStatus(id, status);
        return ResponseEntity.ok(ResponseWrapper.success(enrollmentDTO, "Enrollment status updated"));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete enrollment")
    public ResponseEntity<ResponseWrapper<Void>> deleteEnrollment(@PathVariable UUID id) {
        enrollmentService.deleteEnrollment(id);
        return ResponseEntity.ok(ResponseWrapper.success(null, "Enrollment deleted successfully"));
    }
}
