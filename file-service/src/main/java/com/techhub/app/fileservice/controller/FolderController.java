package com.techhub.app.fileservice.controller;

import com.techhub.app.commonservice.payload.GlobalResponse;
import com.techhub.app.fileservice.dto.request.CreateFolderRequest;
import com.techhub.app.fileservice.dto.request.UpdateFolderRequest;
import com.techhub.app.fileservice.dto.response.FolderResponse;
import com.techhub.app.fileservice.service.FolderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;
import java.util.UUID;
import javax.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/api/folders")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = { "http://localhost:3000", "http://localhost:3001" })
public class FolderController {

    private final FolderService folderService;

    @PostMapping
    public ResponseEntity<GlobalResponse<FolderResponse>> createFolder(@Valid @RequestBody CreateFolderRequest request,
            HttpServletRequest httpRequest) {
        log.info("Creating folder: {} for user: {}", request.getName(), request.getUserId());
        FolderResponse response = folderService.createFolder(request);

        return ResponseEntity.ok(
                GlobalResponse.success("Folder created successfully", response)
                        .withPath(httpRequest.getRequestURI()));
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<GlobalResponse<List<FolderResponse>>> getFoldersByUser(@PathVariable UUID userId,
            HttpServletRequest request) {
        log.info("Getting folders for user: {}", userId);
        List<FolderResponse> folders = folderService.getFoldersByUser(userId);

        return ResponseEntity.ok(
                GlobalResponse.success("Folders retrieved successfully", folders)
                        .withPath(request.getRequestURI()));
    }

    @GetMapping("/{folderId}")
    public ResponseEntity<GlobalResponse<FolderResponse>> getFolder(@PathVariable UUID folderId,
            @RequestParam UUID userId,
            HttpServletRequest request) {
        log.info("Getting folder: {} for user: {}", folderId, userId);
        FolderResponse folder = folderService.getFolderById(userId, folderId);

        return ResponseEntity.ok(
                GlobalResponse.success("Folder retrieved successfully", folder)
                        .withPath(request.getRequestURI()));
    }

    @GetMapping("/{folderId}/tree")
    public ResponseEntity<GlobalResponse<FolderResponse>> getFolderTree(@PathVariable UUID folderId,
            @RequestParam UUID userId,
            HttpServletRequest request) {
        log.info("Getting folder tree: {} for user: {}", folderId, userId);
        FolderResponse folderTree = folderService.getFolderTree(userId, folderId);

        return ResponseEntity.ok(
                GlobalResponse.success("Folder tree retrieved successfully", folderTree)
                        .withPath(request.getRequestURI()));
    }

    @PutMapping("/{folderId}")
    public ResponseEntity<GlobalResponse<FolderResponse>> updateFolder(@PathVariable UUID folderId,
            @RequestParam UUID userId,
            @RequestBody UpdateFolderRequest request,
            HttpServletRequest httpRequest) {
        log.info("Updating folder: {} by user: {}", folderId, userId);
        FolderResponse response = folderService.updateFolder(userId, folderId, request);

        return ResponseEntity.ok(
                GlobalResponse.success("Folder updated successfully", response)
                        .withPath(httpRequest.getRequestURI()));
    }

    @DeleteMapping("/{folderId}")
    public ResponseEntity<GlobalResponse<Void>> deleteFolder(@PathVariable UUID folderId,
            @RequestParam UUID userId,
            HttpServletRequest request) {
        log.info("Deleting folder: {} by user: {}", folderId, userId);
        folderService.deleteFolder(userId, folderId);

        return ResponseEntity.ok(
                GlobalResponse.<Void>success("Folder deleted successfully", null)
                        .withPath(request.getRequestURI()));
    }
}
