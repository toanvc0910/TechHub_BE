package com.techhub.app.fileservice.controller;

import com.techhub.app.fileservice.dto.response.FileResponse;
import com.techhub.app.fileservice.dto.response.FileStatisticsResponse;
import com.techhub.app.fileservice.service.FileManagementService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/files")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = { "http://localhost:3000", "http://localhost:3001" })
public class FileController {

    private final FileManagementService fileManagementService;

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Map<String, Object>> uploadFile(
            @RequestParam("file") MultipartFile file,
            @RequestParam("userId") UUID userId,
            @RequestParam(value = "folderId", required = false) UUID folderId,
            @RequestParam(value = "tags", required = false) String[] tags,
            @RequestParam(value = "description", required = false) String description) {

        log.info("Uploading file: {} by user: {}", file.getOriginalFilename(), userId);

        try {
            FileResponse response = fileManagementService.uploadFile(file, userId, folderId, tags, description);

            Map<String, Object> result = new HashMap<>();
            result.put("status", "success");
            result.put("message", "File uploaded successfully");
            result.put("data", response);

            return ResponseEntity.ok(result);

        } catch (IllegalArgumentException e) {
            log.error("Validation error: {}", e.getMessage());
            Map<String, Object> error = new HashMap<>();
            error.put("status", "error");
            error.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(error);

        } catch (Exception e) {
            log.error("Error uploading file", e);
            Map<String, Object> error = new HashMap<>();
            error.put("status", "error");
            error.put("message", "Failed to upload file: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    @PostMapping("/upload/multiple")
    public ResponseEntity<Map<String, Object>> uploadMultipleFiles(
            @RequestParam("files") List<MultipartFile> files,
            @RequestParam("userId") UUID userId,
            @RequestParam(value = "folderId", required = false) UUID folderId,
            @RequestParam(value = "tags", required = false) String[] tags,
            @RequestParam(value = "description", required = false) String description) {

        log.info("Uploading {} files by user: {}", files.size(), userId);

        try {
            List<FileResponse> responses = fileManagementService.uploadMultipleFiles(files, userId, folderId, tags,
                    description);

            Map<String, Object> result = new HashMap<>();
            result.put("status", "success");
            result.put("message", "Files uploaded successfully");
            result.put("data", responses);

            return ResponseEntity.ok(result);

        } catch (Exception e) {
            log.error("Error uploading files", e);
            Map<String, Object> error = new HashMap<>();
            error.put("status", "error");
            error.put("message", "Failed to upload files: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    @DeleteMapping("/{fileId}")
    public ResponseEntity<Map<String, Object>> deleteFile(
            @PathVariable UUID fileId,
            @RequestParam UUID userId) {
        log.info("Deleting file: {} by user: {}", fileId, userId);

        try {
            fileManagementService.deleteFile(userId, fileId);

            Map<String, Object> result = new HashMap<>();
            result.put("status", "success");
            result.put("message", "File deleted successfully");

            return ResponseEntity.ok(result);

        } catch (RuntimeException e) {
            log.error("Error deleting file: {}", e.getMessage(), e);
            Map<String, Object> error = new HashMap<>();
            error.put("status", "error");
            error.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);

        } catch (Exception e) {
            log.error("Error deleting file", e);
            Map<String, Object> error = new HashMap<>();
            error.put("status", "error");
            error.put("message", "Failed to delete file: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    @GetMapping("/{fileId}")
    public ResponseEntity<Map<String, Object>> getFile(
            @PathVariable UUID fileId,
            @RequestParam UUID userId) {
        log.info("Getting file: {} for user: {}", fileId, userId);

        try {
            FileResponse response = fileManagementService.getFileById(userId, fileId);

            Map<String, Object> result = new HashMap<>();
            result.put("status", "success");
            result.put("data", response);

            return ResponseEntity.ok(result);

        } catch (Exception e) {
            log.error("Error getting file", e);
            Map<String, Object> error = new HashMap<>();
            error.put("status", "error");
            error.put("message", "Failed to get file: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
        }
    }

    @GetMapping("/folder/{folderId}")
    public ResponseEntity<Map<String, Object>> getFilesByFolder(
            @PathVariable UUID folderId,
            @RequestParam UUID userId) {
        log.info("Getting files in folder: {} for user: {}", folderId, userId);

        try {
            List<FileResponse> files = fileManagementService.getFilesByFolder(userId, folderId);

            Map<String, Object> result = new HashMap<>();
            result.put("status", "success");
            result.put("data", files);

            return ResponseEntity.ok(result);

        } catch (Exception e) {
            log.error("Error getting files by folder", e);
            Map<String, Object> error = new HashMap<>();
            error.put("status", "error");
            error.put("message", "Failed to get files: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    @GetMapping
    public ResponseEntity<Map<String, Object>> listFiles(
            @RequestParam UUID userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        log.info("Listing files for user: {}", userId);

        try {
            Page<FileResponse> files = fileManagementService.getFilesByUser(userId, PageRequest.of(page, size));

            Map<String, Object> result = new HashMap<>();
            result.put("status", "success");
            result.put("data", files);

            return ResponseEntity.ok(result);

        } catch (Exception e) {
            log.error("Error listing files", e);
            Map<String, Object> error = new HashMap<>();
            error.put("status", "error");
            error.put("message", "Failed to list files: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    @GetMapping("/statistics")
    public ResponseEntity<Map<String, Object>> getStatistics(@RequestParam UUID userId) {
        log.info("Getting file statistics for user: {}", userId);

        try {
            FileStatisticsResponse stats = fileManagementService.getFileStatistics(userId);

            Map<String, Object> result = new HashMap<>();
            result.put("status", "success");
            result.put("data", stats);

            return ResponseEntity.ok(result);

        } catch (Exception e) {
            log.error("Error getting statistics", e);
            Map<String, Object> error = new HashMap<>();
            error.put("status", "error");
            error.put("message", "Failed to get statistics: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }
}
