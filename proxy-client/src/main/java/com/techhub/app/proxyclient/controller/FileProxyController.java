package com.techhub.app.proxyclient.controller;

import com.techhub.app.proxyclient.client.FileServiceClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/proxy/files")
@RequiredArgsConstructor
@Slf4j
public class FileProxyController {

    private final FileServiceClient fileServiceClient;

    // ==================== FILE MANAGEMENT ====================

    @PostMapping("/upload")
    public ResponseEntity<Map<String, Object>> uploadFile(
            @RequestParam("file") MultipartFile file,
            @RequestParam("userId") UUID userId,
            @RequestParam(value = "folderId", required = false) UUID folderId,
            @RequestParam(value = "tags", required = false) String[] tags,
            @RequestParam(value = "description", required = false) String description) {

        log.info("[PROXY] Uploading file: {} by user: {}", file.getOriginalFilename(), userId);

        try {
            ResponseEntity<Map<String, Object>> response = fileServiceClient.uploadFile(
                    file, userId, folderId, tags, description);
            log.info("[PROXY] File uploaded successfully: {}", file.getOriginalFilename());
            return response;
        } catch (Exception e) {
            log.error("[PROXY] Error uploading file: {}", e.getMessage(), e);
            throw e;
        }
    }

    @PostMapping("/upload/multiple")
    public ResponseEntity<Map<String, Object>> uploadMultipleFiles(
            @RequestParam("files") List<MultipartFile> files,
            @RequestParam("userId") UUID userId,
            @RequestParam(value = "folderId", required = false) UUID folderId,
            @RequestParam(value = "tags", required = false) String[] tags,
            @RequestParam(value = "description", required = false) String description) {

        log.info("[PROXY] Uploading {} files by user: {}", files.size(), userId);

        try {
            ResponseEntity<Map<String, Object>> response = fileServiceClient.uploadMultipleFiles(
                    files, userId, folderId, tags, description);
            log.info("[PROXY] {} files uploaded successfully", files.size());
            return response;
        } catch (Exception e) {
            log.error("[PROXY] Error uploading files: {}", e.getMessage(), e);
            throw e;
        }
    }

    @GetMapping("/{fileId}")
    public ResponseEntity<Map<String, Object>> getFile(
            @PathVariable UUID fileId,
            @RequestParam UUID userId) {
        log.info("[PROXY] Getting file: {} for user: {}", fileId, userId);

        try {
            ResponseEntity<Map<String, Object>> response = fileServiceClient.getFile(fileId, userId);
            log.info("[PROXY] File retrieved: {}", fileId);
            return response;
        } catch (Exception e) {
            log.error("[PROXY] Error getting file: {}", e.getMessage(), e);
            throw e;
        }
    }

    @GetMapping
    public ResponseEntity<Map<String, Object>> listFiles(
            @RequestParam UUID userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        log.info("[PROXY] Listing files for user: {}", userId);

        try {
            ResponseEntity<Map<String, Object>> response = fileServiceClient.listFiles(userId, page, size);
            log.info("[PROXY] Files listed for user: {}", userId);
            return response;
        } catch (Exception e) {
            log.error("[PROXY] Error listing files: {}", e.getMessage(), e);
            throw e;
        }
    }

    @GetMapping("/folder/{folderId}")
    public ResponseEntity<Map<String, Object>> getFilesByFolder(
            @PathVariable UUID folderId,
            @RequestParam UUID userId) {
        log.info("[PROXY] Getting files in folder: {} for user: {}", folderId, userId);

        try {
            ResponseEntity<Map<String, Object>> response = fileServiceClient.getFilesByFolder(folderId, userId);
            log.info("[PROXY] Files retrieved for folder: {}", folderId);
            return response;
        } catch (Exception e) {
            log.error("[PROXY] Error getting files by folder: {}", e.getMessage(), e);
            throw e;
        }
    }

    @DeleteMapping("/{fileId}")
    public ResponseEntity<Map<String, Object>> deleteFile(
            @PathVariable UUID fileId,
            @RequestParam UUID userId) {
        log.info("[PROXY] Deleting file: {} by user: {}", fileId, userId);

        try {
            ResponseEntity<Map<String, Object>> response = fileServiceClient.deleteFile(fileId, userId);
            log.info("[PROXY] File deleted successfully: {}", fileId);
            return response;
        } catch (Exception e) {
            log.error("[PROXY] Error deleting file: {}", e.getMessage(), e);
            throw e;
        }
    }

    @GetMapping("/statistics")
    public ResponseEntity<Map<String, Object>> getStatistics(@RequestParam UUID userId) {
        log.info("[PROXY] Getting file statistics for user: {}", userId);

        try {
            ResponseEntity<Map<String, Object>> response = fileServiceClient.getStatistics(userId);
            log.info("[PROXY] Statistics retrieved for user: {}", userId);
            return response;
        } catch (Exception e) {
            log.error("[PROXY] Error getting statistics: {}", e.getMessage(), e);
            throw e;
        }
    }

    // ==================== FOLDER MANAGEMENT ====================

    @PostMapping("/folders")
    public ResponseEntity<Map<String, Object>> createFolder(@RequestBody Map<String, Object> request) {
        log.info("[PROXY] Creating folder: {}", request.get("name"));

        try {
            ResponseEntity<Map<String, Object>> response = fileServiceClient.createFolder(request);
            log.info("[PROXY] Folder created successfully");
            return response;
        } catch (Exception e) {
            log.error("[PROXY] Error creating folder: {}", e.getMessage(), e);
            throw e;
        }
    }

    @GetMapping("/folders/user/{userId}")
    public ResponseEntity<Map<String, Object>> getFoldersByUser(@PathVariable UUID userId) {
        log.info("[PROXY] Getting folders for user: {}", userId);

        try {
            ResponseEntity<Map<String, Object>> response = fileServiceClient.getFoldersByUser(userId);
            log.info("[PROXY] Folders retrieved for user: {}", userId);
            return response;
        } catch (Exception e) {
            log.error("[PROXY] Error getting folders: {}", e.getMessage(), e);
            throw e;
        }
    }

    @GetMapping("/folders/{folderId}")
    public ResponseEntity<Map<String, Object>> getFolder(
            @PathVariable UUID folderId,
            @RequestParam UUID userId) {
        log.info("[PROXY] Getting folder: {} for user: {}", folderId, userId);

        try {
            ResponseEntity<Map<String, Object>> response = fileServiceClient.getFolder(folderId, userId);
            log.info("[PROXY] Folder retrieved: {}", folderId);
            return response;
        } catch (Exception e) {
            log.error("[PROXY] Error getting folder: {}", e.getMessage(), e);
            throw e;
        }
    }

    @GetMapping("/folders/{folderId}/tree")
    public ResponseEntity<Map<String, Object>> getFolderTree(
            @PathVariable UUID folderId,
            @RequestParam UUID userId) {
        log.info("[PROXY] Getting folder tree: {} for user: {}", folderId, userId);

        try {
            ResponseEntity<Map<String, Object>> response = fileServiceClient.getFolderTree(folderId, userId);
            log.info("[PROXY] Folder tree retrieved: {}", folderId);
            return response;
        } catch (Exception e) {
            log.error("[PROXY] Error getting folder tree: {}", e.getMessage(), e);
            throw e;
        }
    }

    @PutMapping("/folders/{folderId}")
    public ResponseEntity<Map<String, Object>> updateFolder(
            @PathVariable UUID folderId,
            @RequestParam UUID userId,
            @RequestBody Map<String, Object> request) {
        log.info("[PROXY] Updating folder: {} by user: {}", folderId, userId);

        try {
            ResponseEntity<Map<String, Object>> response = fileServiceClient.updateFolder(folderId, userId, request);
            log.info("[PROXY] Folder updated successfully: {}", folderId);
            return response;
        } catch (Exception e) {
            log.error("[PROXY] Error updating folder: {}", e.getMessage(), e);
            throw e;
        }
    }

    @DeleteMapping("/folders/{folderId}")
    public ResponseEntity<Map<String, Object>> deleteFolder(
            @PathVariable UUID folderId,
            @RequestParam UUID userId) {
        log.info("[PROXY] Deleting folder: {} by user: {}", folderId, userId);

        try {
            ResponseEntity<Map<String, Object>> response = fileServiceClient.deleteFolder(folderId, userId);
            log.info("[PROXY] Folder deleted successfully: {}", folderId);
            return response;
        } catch (Exception e) {
            log.error("[PROXY] Error deleting folder: {}", e.getMessage(), e);
            throw e;
        }
    }

    // ==================== FILE USAGE TRACKING ====================

    @PostMapping("/usage/track")
    public ResponseEntity<Map<String, Object>> trackFileUsage(@RequestBody Map<String, Object> request) {
        log.info("[PROXY] Tracking file usage: file={}, type={}, id={}",
                request.get("fileId"), request.get("usedInType"), request.get("usedInId"));

        try {
            ResponseEntity<Map<String, Object>> response = fileServiceClient.trackFileUsage(request);
            log.info("[PROXY] File usage tracked successfully");
            return response;
        } catch (Exception e) {
            log.error("[PROXY] Error tracking file usage: {}", e.getMessage(), e);
            throw e;
        }
    }

    @DeleteMapping("/usage/remove")
    public ResponseEntity<Map<String, Object>> removeFileUsage(
            @RequestParam UUID fileId,
            @RequestParam String usedInType,
            @RequestParam UUID usedInId) {
        log.info("[PROXY] Removing file usage: file={}, type={}, id={}", fileId, usedInType, usedInId);

        try {
            ResponseEntity<Map<String, Object>> response = fileServiceClient.removeFileUsage(
                    fileId, usedInType, usedInId);
            log.info("[PROXY] File usage removed successfully");
            return response;
        } catch (Exception e) {
            log.error("[PROXY] Error removing file usage: {}", e.getMessage(), e);
            throw e;
        }
    }

    @DeleteMapping("/usage/remove-all")
    public ResponseEntity<Map<String, Object>> removeAllFileUsage(
            @RequestParam String usedInType,
            @RequestParam UUID usedInId) {
        log.info("[PROXY] Removing all file usages: type={}, id={}", usedInType, usedInId);

        try {
            ResponseEntity<Map<String, Object>> response = fileServiceClient.removeAllFileUsage(
                    usedInType, usedInId);
            log.info("[PROXY] All file usages removed successfully");
            return response;
        } catch (Exception e) {
            log.error("[PROXY] Error removing all file usages: {}", e.getMessage(), e);
            throw e;
        }
    }

    @GetMapping("/usage/file/{fileId}")
    public ResponseEntity<Map<String, Object>> listFileUsages(@PathVariable UUID fileId) {
        log.info("[PROXY] Listing file usages for file: {}", fileId);

        try {
            ResponseEntity<Map<String, Object>> response = fileServiceClient.listFileUsages(fileId);
            log.info("[PROXY] File usages retrieved for file: {}", fileId);
            return response;
        } catch (Exception e) {
            log.error("[PROXY] Error listing file usages: {}", e.getMessage(), e);
            throw e;
        }
    }
}
