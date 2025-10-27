package com.techhub.app.fileservice.controller;

import com.techhub.app.fileservice.dto.request.CreateFolderRequest;
import com.techhub.app.fileservice.dto.request.UpdateFolderRequest;
import com.techhub.app.fileservice.dto.response.FolderResponse;
import com.techhub.app.fileservice.service.FolderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/folders")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = { "http://localhost:3000", "http://localhost:3001" })
public class FolderController {

    private final FolderService folderService;

    @PostMapping
    public ResponseEntity<Map<String, Object>> createFolder(@Valid @RequestBody CreateFolderRequest request) {
        log.info("Creating folder: {} for user: {}", request.getName(), request.getUserId());

        try {
            FolderResponse response = folderService.createFolder(request);

            Map<String, Object> result = new HashMap<>();
            result.put("status", "success");
            result.put("message", "Folder created successfully");
            result.put("data", response);

            return ResponseEntity.ok(result);

        } catch (Exception e) {
            log.error("Error creating folder", e);
            Map<String, Object> error = new HashMap<>();
            error.put("status", "error");
            error.put("message", "Failed to create folder: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<Map<String, Object>> getFoldersByUser(@PathVariable UUID userId) {
        log.info("Getting folders for user: {}", userId);

        try {
            List<FolderResponse> folders = folderService.getFoldersByUser(userId);

            Map<String, Object> result = new HashMap<>();
            result.put("status", "success");
            result.put("data", folders);

            return ResponseEntity.ok(result);

        } catch (Exception e) {
            log.error("Error getting folders", e);
            Map<String, Object> error = new HashMap<>();
            error.put("status", "error");
            error.put("message", "Failed to get folders: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    @GetMapping("/{folderId}")
    public ResponseEntity<Map<String, Object>> getFolder(@PathVariable UUID folderId, @RequestParam UUID userId) {
        log.info("Getting folder: {} for user: {}", folderId, userId);

        try {
            FolderResponse folder = folderService.getFolderById(userId, folderId);

            Map<String, Object> result = new HashMap<>();
            result.put("status", "success");
            result.put("data", folder);

            return ResponseEntity.ok(result);

        } catch (Exception e) {
            log.error("Error getting folder", e);
            Map<String, Object> error = new HashMap<>();
            error.put("status", "error");
            error.put("message", "Failed to get folder: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
        }
    }

    @GetMapping("/{folderId}/tree")
    public ResponseEntity<Map<String, Object>> getFolderTree(@PathVariable UUID folderId, @RequestParam UUID userId) {
        log.info("Getting folder tree: {} for user: {}", folderId, userId);

        try {
            FolderResponse folderTree = folderService.getFolderTree(userId, folderId);

            Map<String, Object> result = new HashMap<>();
            result.put("status", "success");
            result.put("data", folderTree);

            return ResponseEntity.ok(result);

        } catch (Exception e) {
            log.error("Error getting folder tree", e);
            Map<String, Object> error = new HashMap<>();
            error.put("status", "error");
            error.put("message", "Failed to get folder tree: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    @PutMapping("/{folderId}")
    public ResponseEntity<Map<String, Object>> updateFolder(@PathVariable UUID folderId,
            @RequestParam UUID userId,
            @RequestBody UpdateFolderRequest request) {
        log.info("Updating folder: {} by user: {}", folderId, userId);

        try {
            FolderResponse response = folderService.updateFolder(userId, folderId, request);

            Map<String, Object> result = new HashMap<>();
            result.put("status", "success");
            result.put("message", "Folder updated successfully");
            result.put("data", response);

            return ResponseEntity.ok(result);

        } catch (Exception e) {
            log.error("Error updating folder", e);
            Map<String, Object> error = new HashMap<>();
            error.put("status", "error");
            error.put("message", "Failed to update folder: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    @DeleteMapping("/{folderId}")
    public ResponseEntity<Map<String, Object>> deleteFolder(@PathVariable UUID folderId, @RequestParam UUID userId) {
        log.info("Deleting folder: {} by user: {}", folderId, userId);

        try {
            folderService.deleteFolder(userId, folderId);

            Map<String, Object> result = new HashMap<>();
            result.put("status", "success");
            result.put("message", "Folder deleted successfully");

            return ResponseEntity.ok(result);

        } catch (Exception e) {
            log.error("Error deleting folder", e);
            Map<String, Object> error = new HashMap<>();
            error.put("status", "error");
            error.put("message", "Failed to delete folder: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }
}
