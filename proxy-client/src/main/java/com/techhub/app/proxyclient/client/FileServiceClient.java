package com.techhub.app.proxyclient.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@FeignClient(name = "FILE-SERVICE")
public interface FileServiceClient {

        // ==================== FILE MANAGEMENT ====================

        @PostMapping(value = "/api/files/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
        ResponseEntity<Map<String, Object>> uploadFile(
                        @RequestPart("file") MultipartFile file,
                        @RequestParam("userId") UUID userId,
                        @RequestParam(value = "folderId", required = false) UUID folderId,
                        @RequestParam(value = "tags", required = false) String[] tags,
                        @RequestParam(value = "description", required = false) String description);

        @PostMapping(value = "/api/files/upload/multiple", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
        ResponseEntity<Map<String, Object>> uploadMultipleFiles(
                        @RequestPart("files") List<MultipartFile> files,
                        @RequestParam("userId") UUID userId,
                        @RequestParam(value = "folderId", required = false) UUID folderId,
                        @RequestParam(value = "tags", required = false) String[] tags,
                        @RequestParam(value = "description", required = false) String description);

        @GetMapping("/api/files/{fileId}")
        ResponseEntity<Map<String, Object>> getFile(
                        @PathVariable("fileId") UUID fileId,
                        @RequestParam("userId") UUID userId);

        @GetMapping("/api/files")
        ResponseEntity<Map<String, Object>> listFiles(
                        @RequestParam("userId") UUID userId,
                        @RequestParam(defaultValue = "0") int page,
                        @RequestParam(defaultValue = "20") int size);

        @GetMapping("/api/files/folder/{folderId}")
        ResponseEntity<Map<String, Object>> getFilesByFolder(
                        @PathVariable("folderId") UUID folderId,
                        @RequestParam("userId") UUID userId);

        @DeleteMapping("/api/files/{fileId}")
        ResponseEntity<Map<String, Object>> deleteFile(
                        @PathVariable("fileId") UUID fileId,
                        @RequestParam("userId") UUID userId);

        @GetMapping("/api/files/statistics")
        ResponseEntity<Map<String, Object>> getStatistics(@RequestParam("userId") UUID userId);

        // ==================== FOLDER MANAGEMENT ====================

        @PostMapping("/api/folders")
        ResponseEntity<Map<String, Object>> createFolder(@RequestBody Map<String, Object> request);

        @GetMapping("/api/folders/user/{userId}")
        ResponseEntity<Map<String, Object>> getFoldersByUser(@PathVariable("userId") UUID userId);

        @GetMapping("/api/folders/{folderId}")
        ResponseEntity<Map<String, Object>> getFolder(
                        @PathVariable("folderId") UUID folderId,
                        @RequestParam("userId") UUID userId);

        @GetMapping("/api/folders/{folderId}/tree")
        ResponseEntity<Map<String, Object>> getFolderTree(
                        @PathVariable("folderId") UUID folderId,
                        @RequestParam("userId") UUID userId);

        @PutMapping("/api/folders/{folderId}")
        ResponseEntity<Map<String, Object>> updateFolder(
                        @PathVariable("folderId") UUID folderId,
                        @RequestParam("userId") UUID userId,
                        @RequestBody Map<String, Object> request);

        @DeleteMapping("/api/folders/{folderId}")
        ResponseEntity<Map<String, Object>> deleteFolder(
                        @PathVariable("folderId") UUID folderId,
                        @RequestParam("userId") UUID userId);

        // ==================== FILE USAGE TRACKING ====================

        @PostMapping("/api/file-usage/track")
        ResponseEntity<Map<String, Object>> trackFileUsage(@RequestBody Map<String, Object> request);

        @DeleteMapping("/api/file-usage/remove")
        ResponseEntity<Map<String, Object>> removeFileUsage(
                        @RequestParam("fileId") UUID fileId,
                        @RequestParam("usedInType") String usedInType,
                        @RequestParam("usedInId") UUID usedInId);

        @DeleteMapping("/api/file-usage/remove-all")
        ResponseEntity<Map<String, Object>> removeAllFileUsage(
                        @RequestParam("usedInType") String usedInType,
                        @RequestParam("usedInId") UUID usedInId);

        @GetMapping("/api/file-usage/file/{fileId}")
        ResponseEntity<Map<String, Object>> listFileUsages(@PathVariable("fileId") UUID fileId);
}
