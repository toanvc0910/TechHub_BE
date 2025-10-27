package com.techhub.app.fileservice.controller;

import com.techhub.app.fileservice.dto.response.FileUploadResponse;
import com.techhub.app.fileservice.service.FileService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/files")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = {"http://localhost:3000", "http://localhost:3001"})
public class FileController {

    private final FileService fileService;

    @PostMapping("/upload")
    public ResponseEntity<Map<String, Object>> uploadFile(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "folder", defaultValue = "uploads") String folder) {
        
        log.info("Uploading file: {} to folder: {}", file.getOriginalFilename(), folder);
        
        try {
            FileUploadResponse response = fileService.uploadFile(file, folder);
            
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
            @RequestParam(value = "folder", defaultValue = "uploads") String folder) {
        
        log.info("Uploading {} files to folder: {}", files.size(), folder);
        
        try {
            List<FileUploadResponse> responses = fileService.uploadMultipleFiles(files, folder);
            
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

    @DeleteMapping("/{publicId}")
    public ResponseEntity<Map<String, Object>> deleteFile(@PathVariable String publicId) {
        log.info("Deleting file with publicId: {}", publicId);
        
        try {
            fileService.deleteFile(publicId);
            
            Map<String, Object> result = new HashMap<>();
            result.put("status", "success");
            result.put("message", "File deleted successfully");
            
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            log.error("Error deleting file", e);
            Map<String, Object> error = new HashMap<>();
            error.put("status", "error");
            error.put("message", "Failed to delete file: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<Map<String, Object>> getFileMetadata(@PathVariable Long id) {
        log.info("Getting metadata for file id: {}", id);
        
        try {
            FileUploadResponse response = fileService.getFileMetadata(id);
            
            Map<String, Object> result = new HashMap<>();
            result.put("status", "success");
            result.put("data", response);
            
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            log.error("Error getting file metadata", e);
            Map<String, Object> error = new HashMap<>();
            error.put("status", "error");
            error.put("message", "Failed to get file metadata: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
        }
    }
}
