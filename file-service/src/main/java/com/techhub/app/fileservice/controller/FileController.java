package com.techhub.app.fileservice.controller;

import com.techhub.app.commonservice.payload.GlobalResponse;
import com.techhub.app.commonservice.payload.PageGlobalResponse;
import com.techhub.app.fileservice.dto.response.FileResponse;
import com.techhub.app.fileservice.dto.response.FileStatisticsResponse;
import com.techhub.app.fileservice.service.FileManagementService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;
import javax.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/api/files")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = { "http://localhost:3000", "http://localhost:3001" })
public class FileController {

    private final FileManagementService fileManagementService;

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<GlobalResponse<FileResponse>> uploadFile(
            @RequestParam("file") MultipartFile file,
            @RequestParam("userId") UUID userId,
            @RequestParam(value = "folderId", required = false) UUID folderId,
            @RequestParam(value = "tags", required = false) String[] tags,
            @RequestParam(value = "description", required = false) String description,
            HttpServletRequest request) {

        log.info("Uploading file: {} by user: {}", file.getOriginalFilename(), userId);

        FileResponse response = fileManagementService.uploadFile(file, userId, folderId, tags, description);

        return ResponseEntity.ok(
                GlobalResponse.success("File uploaded successfully", response)
                        .withPath(request.getRequestURI()));
    }

    @PostMapping("/upload/multiple")
    public ResponseEntity<GlobalResponse<List<FileResponse>>> uploadMultipleFiles(
            @RequestParam("files") List<MultipartFile> files,
            @RequestParam("userId") UUID userId,
            @RequestParam(value = "folderId", required = false) UUID folderId,
            @RequestParam(value = "tags", required = false) String[] tags,
            @RequestParam(value = "description", required = false) String description,
            HttpServletRequest request) {

        log.info("Uploading {} files by user: {}", files.size(), userId);

        List<FileResponse> responses = fileManagementService.uploadMultipleFiles(files, userId, folderId, tags,
                description);

        return ResponseEntity.ok(
                GlobalResponse.success("Files uploaded successfully", responses)
                        .withPath(request.getRequestURI()));
    }

    @DeleteMapping("/{fileId}")
    public ResponseEntity<GlobalResponse<Void>> deleteFile(
            @PathVariable UUID fileId,
            @RequestParam UUID userId,
            HttpServletRequest request) {
        log.info("Deleting file: {} by user: {}", fileId, userId);
        fileManagementService.deleteFile(userId, fileId);

        return ResponseEntity.ok(
                GlobalResponse.<Void>success("File deleted successfully", null)
                        .withPath(request.getRequestURI()));
    }

    @GetMapping("/{fileId}")
    public ResponseEntity<GlobalResponse<FileResponse>> getFile(
            @PathVariable UUID fileId,
            @RequestParam UUID userId,
            HttpServletRequest request) {
        log.info("Getting file: {} for user: {}", fileId, userId);
        FileResponse response = fileManagementService.getFileById(userId, fileId);

        return ResponseEntity.ok(
                GlobalResponse.success("File retrieved successfully", response)
                        .withPath(request.getRequestURI()));
    }

    @GetMapping("/folder/{folderId}")
    public ResponseEntity<GlobalResponse<List<FileResponse>>> getFilesByFolder(
            @PathVariable UUID folderId,
            @RequestParam UUID userId,
            HttpServletRequest request) {
        log.info("Getting files in folder: {} for user: {}", folderId, userId);
        List<FileResponse> files = fileManagementService.getFilesByFolder(userId, folderId);

        return ResponseEntity.ok(
                GlobalResponse.success("Files retrieved successfully", files)
                        .withPath(request.getRequestURI()));
    }

    @GetMapping
    public ResponseEntity<PageGlobalResponse<FileResponse>> listFiles(
            @RequestParam UUID userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            HttpServletRequest request) {
        log.info("Listing files for user: {}", userId);
        Page<FileResponse> files = fileManagementService.getFilesByUser(userId, PageRequest.of(page, size));

        PageGlobalResponse.PaginationInfo paginationInfo = PageGlobalResponse.PaginationInfo.builder()
                .page(files.getNumber())
                .size(files.getSize())
                .totalElements(files.getTotalElements())
                .totalPages(files.getTotalPages())
                .first(files.isFirst())
                .last(files.isLast())
                .hasNext(files.hasNext())
                .hasPrevious(files.hasPrevious())
                .build();

        return ResponseEntity.ok(
                PageGlobalResponse.success("Files listed successfully", files.getContent(), paginationInfo)
                        .withPath(request.getRequestURI()));
    }

    @GetMapping("/statistics")
    public ResponseEntity<GlobalResponse<FileStatisticsResponse>> getStatistics(@RequestParam UUID userId,
            HttpServletRequest request) {
        log.info("Getting file statistics for user: {}", userId);
        FileStatisticsResponse stats = fileManagementService.getFileStatistics(userId);

        return ResponseEntity.ok(
                GlobalResponse.success("Statistics retrieved successfully", stats)
                        .withPath(request.getRequestURI()));
    }
}
