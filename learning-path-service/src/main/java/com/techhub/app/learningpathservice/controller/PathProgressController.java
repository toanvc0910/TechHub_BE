package com.techhub.app.learningpathservice.controller;

import com.techhub.app.commonservice.payload.GlobalResponse;
import com.techhub.app.commonservice.payload.PageGlobalResponse;
import com.techhub.app.learningpathservice.dto.PathProgressResponseDTO;
import com.techhub.app.learningpathservice.dto.UpdateProgressRequestDTO;
import com.techhub.app.learningpathservice.service.PathProgressService;
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
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/path-progress")
@RequiredArgsConstructor
@Slf4j
@Validated
public class PathProgressController {

    private final PathProgressService pathProgressService;

    @PostMapping
    public ResponseEntity<GlobalResponse<PathProgressResponseDTO>> createOrUpdateProgress(
            @Valid @RequestBody UpdateProgressRequestDTO requestDTO) {
        log.info("REST request to create or update progress for user {} on path {}",
                requestDTO.getUserId(), requestDTO.getPathId());

        PathProgressResponseDTO response = pathProgressService.createOrUpdateProgress(requestDTO);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(GlobalResponse.success("Progress updated successfully", response));
    }

    @GetMapping("/user/{userId}/path/{pathId}")
    public ResponseEntity<GlobalResponse<PathProgressResponseDTO>> getProgressByUserAndPath(
            @PathVariable UUID userId,
            @PathVariable UUID pathId) {
        log.info("REST request to get progress for user {} on path {}", userId, pathId);

        PathProgressResponseDTO response = pathProgressService.getProgressByUserAndPath(userId, pathId);

        return ResponseEntity.ok(GlobalResponse.success(response));
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<PageGlobalResponse<PathProgressResponseDTO>> getProgressByUser(
            @PathVariable UUID userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        log.info("REST request to get all progress for user: {}", userId);

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "updated"));
        Page<PathProgressResponseDTO> responsePage = pathProgressService.getProgressByUser(userId, pageable);

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

        return ResponseEntity.ok(PageGlobalResponse.success("User progress retrieved successfully",
                responsePage.getContent(), paginationInfo));
    }

    @GetMapping("/path/{pathId}")
    public ResponseEntity<PageGlobalResponse<PathProgressResponseDTO>> getProgressByPath(
            @PathVariable UUID pathId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        log.info("REST request to get all progress for path: {}", pathId);

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "completion"));
        Page<PathProgressResponseDTO> responsePage = pathProgressService.getProgressByPath(pathId, pageable);

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

        return ResponseEntity.ok(PageGlobalResponse.success("Path progress retrieved successfully",
                responsePage.getContent(), paginationInfo));
    }

    @DeleteMapping("/user/{userId}/path/{pathId}")
    public ResponseEntity<GlobalResponse<Void>> deleteProgress(
            @PathVariable UUID userId,
            @PathVariable UUID pathId) {
        log.info("REST request to delete progress for user {} on path {}", userId, pathId);

        pathProgressService.deleteProgress(userId, pathId);

        return ResponseEntity.ok(GlobalResponse.success("Progress deleted successfully", null));
    }

    @GetMapping("/path/{pathId}/statistics")
    public ResponseEntity<GlobalResponse<Map<String, Object>>> getPathStatistics(
            @PathVariable UUID pathId) {
        log.info("REST request to get statistics for path: {}", pathId);

        Long enrolledUsers = pathProgressService.countEnrolledUsers(pathId);
        Float averageCompletion = pathProgressService.getAverageCompletion(pathId);

        Map<String, Object> statistics = new HashMap<>();
        statistics.put("enrolledUsers", enrolledUsers);
        statistics.put("averageCompletion", averageCompletion);

        return ResponseEntity.ok(GlobalResponse.success("Path statistics retrieved successfully", statistics));
    }
}
