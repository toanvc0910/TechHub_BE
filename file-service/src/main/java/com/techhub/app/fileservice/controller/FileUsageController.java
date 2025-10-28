package com.techhub.app.fileservice.controller;

import com.techhub.app.fileservice.dto.request.TrackFileUsageRequest;
import com.techhub.app.fileservice.service.FileUsageService;
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
@RequestMapping("/api/file-usage")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = { "http://localhost:3000", "http://localhost:3001" })
public class FileUsageController {

    private final FileUsageService fileUsageService;

    @PostMapping("/track")
    public ResponseEntity<Map<String, Object>> trackUsage(@Valid @RequestBody TrackFileUsageRequest request) {
        log.info("Tracking usage for file: {} in {}: {}", request.getFileId(), request.getUsedInType(),
                request.getUsedInId());

        try {
            fileUsageService.trackUsage(request);

            Map<String, Object> result = new HashMap<>();
            result.put("status", "success");
            result.put("message", "File usage tracked successfully");

            return ResponseEntity.ok(result);

        } catch (Exception e) {
            log.error("Error tracking file usage", e);
            Map<String, Object> error = new HashMap<>();
            error.put("status", "error");
            error.put("message", "Failed to track file usage: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    @DeleteMapping("/remove")
    public ResponseEntity<Map<String, Object>> removeUsage(
            @RequestParam UUID fileId,
            @RequestParam String usedInType,
            @RequestParam UUID usedInId) {
        log.info("Removing usage for file: {} in {}: {}", fileId, usedInType, usedInId);

        try {
            fileUsageService.removeUsage(fileId, usedInType, usedInId);

            Map<String, Object> result = new HashMap<>();
            result.put("status", "success");
            result.put("message", "File usage removed successfully");

            return ResponseEntity.ok(result);

        } catch (Exception e) {
            log.error("Error removing file usage", e);
            Map<String, Object> error = new HashMap<>();
            error.put("status", "error");
            error.put("message", "Failed to remove file usage: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    @DeleteMapping("/remove-all")
    public ResponseEntity<Map<String, Object>> removeAllByUsedIn(
            @RequestParam String usedInType,
            @RequestParam UUID usedInId) {
        log.info("Removing all file usages for {}: {}", usedInType, usedInId);

        try {
            fileUsageService.removeAllByUsedIn(usedInType, usedInId);

            Map<String, Object> result = new HashMap<>();
            result.put("status", "success");
            result.put("message", "All file usages removed successfully");

            return ResponseEntity.ok(result);

        } catch (Exception e) {
            log.error("Error removing file usages", e);
            Map<String, Object> error = new HashMap<>();
            error.put("status", "error");
            error.put("message", "Failed to remove file usages: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    @GetMapping("/file/{fileId}")
    public ResponseEntity<Map<String, Object>> listUsagesByFile(@PathVariable UUID fileId) {
        log.info("Listing usages for file: {}", fileId);

        try {
            List<?> usages = fileUsageService.listUsagesByFile(fileId);

            Map<String, Object> result = new HashMap<>();
            result.put("status", "success");
            result.put("data", usages);

            return ResponseEntity.ok(result);

        } catch (Exception e) {
            log.error("Error listing file usages", e);
            Map<String, Object> error = new HashMap<>();
            error.put("status", "error");
            error.put("message", "Failed to list file usages: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }
}
