package com.techhub.app.fileservice.service;

import com.techhub.app.fileservice.dto.response.FileResponse;
import com.techhub.app.fileservice.dto.response.FileStatisticsResponse;
import com.techhub.app.fileservice.enums.FileTypeEnum;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

public interface FileManagementService {
    FileResponse uploadFile(MultipartFile file, UUID userId, UUID folderId, String[] tags, String description);

    List<FileResponse> uploadMultipleFiles(List<MultipartFile> files, UUID userId, UUID folderId, String[] tags,
            String description);

    FileResponse getFileById(UUID userId, UUID fileId);

    List<FileResponse> getFilesByFolder(UUID userId, UUID folderId);

    Page<FileResponse> getFilesByFolderPaginated(UUID userId, UUID folderId, Pageable pageable);

    Page<FileResponse> getFilesByUser(UUID userId, Pageable pageable);

    Page<FileResponse> getFilesByType(UUID userId, FileTypeEnum fileType, Pageable pageable);

    Page<FileResponse> searchFiles(UUID userId, String keyword, Pageable pageable);

    List<FileResponse> getFilesByTag(UUID userId, String tag);

    FileResponse updateFile(UUID userId, UUID fileId, UUID folderId, String[] tags, String description);

    void deleteFile(UUID userId, UUID fileId);

    void updateFileUsage(UUID fileId);

    FileStatisticsResponse getFileStatistics(UUID userId);
}
