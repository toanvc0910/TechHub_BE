package com.techhub.app.learningpathservice.controller;

import com.techhub.app.commonservice.payload.GlobalResponse;
import com.techhub.app.commonservice.payload.PageGlobalResponse;
import com.techhub.app.learningpathservice.dto.*;
import com.techhub.app.learningpathservice.service.LearningPathService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/learning-paths")
@RequiredArgsConstructor
@Slf4j
@Validated
public class LearningPathController {

    private static final String ROLE_SUPER_ADMIN = "SUPER_ADMIN";
    private static final String ROLE_ADMIN = "ADMIN";

    private final LearningPathService learningPathService;

    @PostMapping
    public ResponseEntity<GlobalResponse<LearningPathResponseDTO>> createLearningPath(
            @Valid @RequestBody LearningPathRequestDTO requestDTO,
            @RequestHeader(value = "X-User-Id", required = false) UUID userId) {
        log.info("REST request to create learning path: {}", requestDTO.getTitle());

        // Stamp ownership from the trusted identity forwarded by the gateway so the
        // creator is authoritative (the create form does not send it).
        if (userId != null) {
            requestDTO.setCreatedBy(userId);
            requestDTO.setUpdatedBy(userId);
        }

        LearningPathResponseDTO response = learningPathService.createLearningPath(requestDTO);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(GlobalResponse.success("Learning path created successfully", response));
    }

    @PutMapping("/{id}")
    public ResponseEntity<GlobalResponse<LearningPathResponseDTO>> updateLearningPath(
            @PathVariable UUID id,
            @Valid @RequestBody LearningPathRequestDTO requestDTO,
            @RequestHeader(value = "X-User-Id", required = false) UUID userId,
            @RequestHeader(value = "X-User-Roles", required = false) String roles) {
        log.info("REST request to update learning path: {}", id);

        if (userId != null) {
            requestDTO.setUpdatedBy(userId);
        }

        LearningPathResponseDTO response = learningPathService.updateLearningPath(id, requestDTO, userId,
                isAdmin(roles));

        return ResponseEntity.ok(GlobalResponse.success("Learning path updated successfully", response));
    }

    @GetMapping("/{id}")
    public ResponseEntity<GlobalResponse<LearningPathResponseDTO>> getLearningPathById(
            @PathVariable UUID id) {
        log.info("REST request to get learning path: {}", id);

        LearningPathResponseDTO response = learningPathService.getLearningPathById(id);

        return ResponseEntity.ok(GlobalResponse.success(response));
    }

    @GetMapping
    public ResponseEntity<PageGlobalResponse<LearningPathResponseDTO>> getAllLearningPaths(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "created") String sortBy,
            @RequestParam(defaultValue = "DESC") String sortDirection) {
        log.info("REST request to get all learning paths - page: {}, size: {}", page, size);

        Sort.Direction direction = Sort.Direction.fromString(sortDirection);
        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortBy));

        Page<LearningPathResponseDTO> responsePage = learningPathService.getAllLearningPaths(pageable);

        PageGlobalResponse.PaginationInfo paginationInfo = PageGlobalResponse.PaginationInfo.builder()
                .page(responsePage.getNumber())
                .size(responsePage.getSize())
                .totalElements(responsePage.getTotalElements())
                .totalPages(responsePage.getTotalPages())
                .first(responsePage.isFirst())
                .last(responsePage.isLast())
                .hasNext(responsePage.hasNext())
                .hasPrevious(responsePage.hasPrevious())
                .build();

        return ResponseEntity.ok(PageGlobalResponse.success("Learning paths retrieved successfully",
                responsePage.getContent(), paginationInfo));
    }

    @GetMapping("/my-paths")
    public ResponseEntity<PageGlobalResponse<LearningPathResponseDTO>> getMyLearningPaths(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "created") String sortBy,
            @RequestParam(defaultValue = "DESC") String sortDirection,
            @RequestHeader(value = "X-User-Id", required = false) UUID userId,
            @RequestHeader(value = "X-User-Roles", required = false) String roles) {
        log.info("REST request to get my learning paths - userId: {}, page: {}, size: {}", userId, page, size);

        Sort.Direction direction = Sort.Direction.fromString(sortDirection);
        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortBy));

        Page<LearningPathResponseDTO> responsePage = learningPathService.getMyLearningPaths(userId, isAdmin(roles),
                pageable);

        PageGlobalResponse.PaginationInfo paginationInfo = PageGlobalResponse.PaginationInfo.builder()
                .page(responsePage.getNumber())
                .size(responsePage.getSize())
                .totalElements(responsePage.getTotalElements())
                .totalPages(responsePage.getTotalPages())
                .first(responsePage.isFirst())
                .last(responsePage.isLast())
                .hasNext(responsePage.hasNext())
                .hasPrevious(responsePage.hasPrevious())
                .build();

        return ResponseEntity.ok(PageGlobalResponse.success("Learning paths retrieved successfully",
                responsePage.getContent(), paginationInfo));
    }

    @GetMapping("/search")
    public ResponseEntity<PageGlobalResponse<LearningPathResponseDTO>> searchLearningPaths(
            @RequestParam String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        log.info("REST request to search learning paths with keyword: {}", keyword);

        Pageable pageable = PageRequest.of(page, size);
        Page<LearningPathResponseDTO> responsePage = learningPathService.searchLearningPaths(keyword, pageable);

        PageGlobalResponse.PaginationInfo paginationInfo = PageGlobalResponse.PaginationInfo.builder()
                .page(responsePage.getNumber())
                .size(responsePage.getSize())
                .totalElements(responsePage.getTotalElements())
                .totalPages(responsePage.getTotalPages())
                .first(responsePage.isFirst())
                .last(responsePage.isLast())
                .hasNext(responsePage.hasNext())
                .hasPrevious(responsePage.hasPrevious())
                .build();

        return ResponseEntity.ok(PageGlobalResponse.success("Search completed successfully",
                responsePage.getContent(), paginationInfo));
    }

    @GetMapping("/creator/{userId}")
    public ResponseEntity<PageGlobalResponse<LearningPathResponseDTO>> getLearningPathsByCreator(
            @PathVariable UUID userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        log.info("REST request to get learning paths by creator: {}", userId);

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "created"));
        Page<LearningPathResponseDTO> responsePage = learningPathService.getLearningPathsByCreator(userId, pageable);

        PageGlobalResponse.PaginationInfo paginationInfo = PageGlobalResponse.PaginationInfo.builder()
                .page(responsePage.getNumber())
                .size(responsePage.getSize())
                .totalElements(responsePage.getTotalElements())
                .totalPages(responsePage.getTotalPages())
                .first(responsePage.isFirst())
                .last(responsePage.isLast())
                .hasNext(responsePage.hasNext())
                .hasPrevious(responsePage.hasPrevious())
                .build();

        return ResponseEntity.ok(PageGlobalResponse.success("Learning paths retrieved successfully",
                responsePage.getContent(), paginationInfo));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<GlobalResponse<Void>> deleteLearningPath(
            @PathVariable UUID id,
            @RequestHeader(value = "X-User-Id", required = false) UUID userId,
            @RequestHeader(value = "X-User-Roles", required = false) String roles) {
        log.info("REST request to delete learning path: {}", id);

        learningPathService.deleteLearningPath(id, userId, isAdmin(roles));

        return ResponseEntity.ok(GlobalResponse.success("Learning path deleted successfully", null));
    }

    /**
     * The gateway forwards the caller's effective role(s) in the X-User-Roles
     * header. ADMIN/SUPER_ADMIN manage every author's learning paths.
     */
    private boolean isAdmin(String rolesHeader) {
        if (rolesHeader == null || rolesHeader.isEmpty()) {
            return false;
        }
        for (String role : rolesHeader.split(",")) {
            String normalized = role.trim();
            if (ROLE_ADMIN.equalsIgnoreCase(normalized) || ROLE_SUPER_ADMIN.equalsIgnoreCase(normalized)) {
                return true;
            }
        }
        return false;
    }

    @PostMapping("/{pathId}/courses")
    public ResponseEntity<GlobalResponse<LearningPathResponseDTO>> addCoursesToPath(
            @PathVariable UUID pathId,
            @Valid @RequestBody AddCoursesToPathRequestDTO requestDTO) {
        log.info("REST request to add courses to learning path: {}", pathId);

        LearningPathResponseDTO response = learningPathService.addCoursesToPath(pathId, requestDTO);

        return ResponseEntity.ok(GlobalResponse.success("Courses added successfully", response));
    }

    @DeleteMapping("/{pathId}/courses/{courseId}")
    public ResponseEntity<GlobalResponse<LearningPathResponseDTO>> removeCourseFromPath(
            @PathVariable UUID pathId,
            @PathVariable UUID courseId) {
        log.info("REST request to remove course {} from learning path {}", courseId, pathId);

        LearningPathResponseDTO response = learningPathService.removeCourseFromPath(pathId, courseId);

        return ResponseEntity.ok(GlobalResponse.success("Course removed successfully", response));
    }

    @PutMapping("/{pathId}/courses/reorder")
    public ResponseEntity<GlobalResponse<LearningPathResponseDTO>> reorderCourses(
            @PathVariable UUID pathId,
            @Valid @RequestBody List<CourseInPathDTO> courses) {
        log.info("=".repeat(100));
        log.info("🌐 REST API - REORDER COURSES REQUEST");
        log.info("REST request to reorder courses in learning path: {}", pathId);
        log.info("📥 Request body contains {} courses", courses != null ? courses.size() : 0);

        if (courses != null) {
            for (int i = 0; i < courses.size(); i++) {
                CourseInPathDTO course = courses.get(i);
                log.info("   Request Course {}: courseId={}, order={}, positionX={}, positionY={}, isOptional={}",
                        (i + 1), course.getCourseId(), course.getOrder(),
                        course.getPositionX(), course.getPositionY(), course.getIsOptional());
            }
        }

        LearningPathResponseDTO response = learningPathService.reorderCourses(pathId, courses);

        log.info("\n📤 Response contains {} courses",
                response.getCourses() != null ? response.getCourses().size() : 0);
        if (response.getCourses() != null) {
            for (CourseInPathDTO course : response.getCourses()) {
                log.info("   Response Course: courseId={}, order={}, positionX={}, positionY={}",
                        course.getCourseId(), course.getOrder(),
                        course.getPositionX(), course.getPositionY());
            }
        }
        log.info("🌐 REST API - REORDER COURSES RESPONSE SENT");
        log.info("=".repeat(100));

        return ResponseEntity.ok(GlobalResponse.success("Courses reordered successfully", response));
    }

    @GetMapping("/by-course/{courseId}")
    public ResponseEntity<GlobalResponse<List<LearningPathResponseDTO>>> getLearningPathsByCourse(
            @PathVariable UUID courseId) {
        log.info("REST request to get learning paths containing course: {}", courseId);

        List<LearningPathResponseDTO> response = learningPathService.getLearningPathsByCourse(courseId);

        return ResponseEntity.ok(GlobalResponse.success("Learning paths retrieved successfully", response));
    }
}
