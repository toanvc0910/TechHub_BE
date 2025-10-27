package com.techhub.app.proxyclient.controller;

import com.techhub.app.proxyclient.client.FileServiceClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/proxy/files")
@RequiredArgsConstructor
@Slf4j
public class FileProxyController {

    private final FileServiceClient fileServiceClient;

    @PostMapping("/upload")
    public ResponseEntity<Map<String, Object>> uploadFile(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "folder", defaultValue = "uploads") String folder) {

        log.info("[PROXY] Uploading file: {} to folder: {}", file.getOriginalFilename(), folder);

        try {
            ResponseEntity<Map<String, Object>> response = fileServiceClient.uploadFile(file, folder);
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
            @RequestParam(value = "folder", defaultValue = "uploads") String folder) {

        log.info("[PROXY] Uploading {} files to folder: {}", files.size(), folder);

        try {
            ResponseEntity<Map<String, Object>> response = fileServiceClient.uploadMultipleFiles(files, folder);
            log.info("[PROXY] {} files uploaded successfully", files.size());
            return response;
        } catch (Exception e) {
            log.error("[PROXY] Error uploading files: {}", e.getMessage(), e);
            throw e;
        }
    }

    @DeleteMapping("/delete")
    public ResponseEntity<Map<String, Object>> deleteFile(@RequestParam("publicId") String publicId) {
        log.info("[PROXY] Deleting file with publicId: {}", publicId);

        try {
            ResponseEntity<Map<String, Object>> response = fileServiceClient.deleteFile(publicId);
            log.info("[PROXY] File deleted successfully: {}", publicId);
            return response;
        } catch (Exception e) {
            log.error("[PROXY] Error deleting file: {}", e.getMessage(), e);
            throw e;
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<Map<String, Object>> getFileMetadata(@PathVariable Long id) {
        log.info("[PROXY] Getting metadata for file id: {}", id);

        try {
            ResponseEntity<Map<String, Object>> response = fileServiceClient.getFileMetadata(id);
            log.info("[PROXY] File metadata retrieved for id: {}", id);
            return response;
        } catch (Exception e) {
            log.error("[PROXY] Error getting file metadata: {}", e.getMessage(), e);
            throw e;
        }
    }
}
