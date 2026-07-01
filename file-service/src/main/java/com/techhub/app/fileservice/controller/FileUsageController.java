package com.techhub.app.fileservice.controller;

import com.techhub.app.commonservice.payload.GlobalResponse;
import com.techhub.app.fileservice.dto.request.TrackFileUsageRequest;
import com.techhub.app.fileservice.service.FileUsageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import javax.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/api/file-usage")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = { "http://localhost:3000", "http://localhost:3001" })
public class FileUsageController {

    private final FileUsageService fileUsageService;

    @PostMapping("/track")
    public ResponseEntity<GlobalResponse<Void>> trackUsage(@Valid @RequestBody TrackFileUsageRequest request,
            HttpServletRequest httpRequest) {
        log.info("Tracking usage for file: {} in {}: {}", request.getFileId(), request.getUsedInType(),
                request.getUsedInId());
        fileUsageService.trackUsage(request);

        return ResponseEntity.ok(
                GlobalResponse.<Void>success("File usage tracked successfully", null)
                        .withPath(httpRequest.getRequestURI()));
    }

    @DeleteMapping("/remove")
    public ResponseEntity<GlobalResponse<Void>> removeUsage(
            @RequestParam UUID fileId,
            @RequestParam String usedInType,
            @RequestParam UUID usedInId,
            HttpServletRequest httpRequest) {
        log.info("Removing usage for file: {} in {}: {}", fileId, usedInType, usedInId);
        fileUsageService.removeUsage(fileId, usedInType, usedInId);

        return ResponseEntity.ok(
                GlobalResponse.<Void>success("File usage removed successfully", null)
                        .withPath(httpRequest.getRequestURI()));
    }

    @DeleteMapping("/remove-all")
    public ResponseEntity<GlobalResponse<Void>> removeAllByUsedIn(
            @RequestParam String usedInType,
            @RequestParam UUID usedInId,
            HttpServletRequest httpRequest) {
        log.info("Removing all file usages for {}: {}", usedInType, usedInId);
        fileUsageService.removeAllByUsedIn(usedInType, usedInId);

        return ResponseEntity.ok(
                GlobalResponse.<Void>success("All file usages removed successfully", null)
                        .withPath(httpRequest.getRequestURI()));
    }

    @GetMapping("/file/{fileId}")
    public ResponseEntity<GlobalResponse<List<Object>>> listUsagesByFile(@PathVariable UUID fileId,
            HttpServletRequest request) {
        log.info("Listing usages for file: {}", fileId);
        List<?> usages = fileUsageService.listUsagesByFile(fileId);
        List<Object> usagePayload = new ArrayList<>(usages);

        return ResponseEntity.ok(
                GlobalResponse.success("File usages retrieved successfully", usagePayload)
                        .withPath(request.getRequestURI()));
    }
}
