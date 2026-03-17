package com.techhub.app.fileservice.service.impl;

import com.techhub.app.fileservice.dto.response.FileResponse;
import com.techhub.app.fileservice.dto.response.FileStatisticsResponse;
import com.techhub.app.fileservice.entity.FileEntity;
import com.techhub.app.fileservice.entity.FileFolderEntity;
import com.techhub.app.fileservice.enums.FileTypeEnum;
import com.techhub.app.fileservice.kafka.FileEventPublisher;
import com.techhub.app.fileservice.kafka.FileUploadedEvent;
import com.techhub.app.fileservice.repository.FileFolderRepository;
import com.techhub.app.fileservice.repository.FileRepository;
import com.techhub.app.fileservice.repository.FileUsageRepository;
import com.techhub.app.fileservice.service.FileManagementService;
import com.techhub.app.fileservice.service.ObjectStorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class FileManagementServiceImpl implements FileManagementService {

    private final ObjectStorageService objectStorageService;
    private final FileRepository fileRepository;
    private final FileFolderRepository folderRepository;
    private final FileUsageRepository usageRepository;
    private final FileEventPublisher fileEventPublisher;

    @Override
    @Transactional
    public FileResponse uploadFile(MultipartFile file, UUID userId, UUID folderId,
            String[] tags, String description) {
        try {
            // Validate folder if provided
            if (folderId != null) {
                folderRepository.findByIdAndUserIdAndIsActive(folderId, userId, "Y")
                        .orElseThrow(() -> new RuntimeException("Folder not found"));
            }

            FileTypeEnum fileType = determineFileType(file.getContentType());
            String objectKey = buildObjectKey(userId, fileType, file.getOriginalFilename());
            StoredObjectDetails storedObject = objectStorageService.upload(file, objectKey);

            // Create file entity
            FileEntity fileEntity = new FileEntity();
            fileEntity.setUserId(userId);
            fileEntity.setFolderId(folderId);
            fileEntity.setName(file.getOriginalFilename());
            fileEntity.setOriginalName(file.getOriginalFilename());
            fileEntity.setFileType(fileType); // Use pre-determined file type
            fileEntity.setMimeType(file.getContentType());
            fileEntity.setFileSize(file.getSize());
            fileEntity.setCloudinaryPublicId(storedObject.getObjectKey());
            fileEntity.setCloudinaryUrl(storedObject.getPublicUrl());
            fileEntity.setCloudinarySecureUrl(storedObject.getPublicUrl());
            fileEntity.setStorageProvider("MINIO");
            fileEntity.setBucketName(storedObject.getBucket());
            fileEntity.setObjectKey(storedObject.getObjectKey());
            fileEntity.setPublicUrl(storedObject.getPublicUrl());
            fileEntity.setSecureUrl(storedObject.getPublicUrl());

            populateImageMetadata(file, fileType, fileEntity);

            fileEntity.setTags(tags);
            fileEntity.setDescription(description);
            fileEntity.setIsActive("Y");
            fileEntity.setCreatedBy(userId);
            fileEntity.setUploadSource("DIRECT");
            initializeProcessingState(fileEntity);

            FileEntity saved = fileRepository.save(fileEntity);
            publishProcessingEventIfNeeded(saved);
            log.info("File uploaded successfully: {}", saved.getId());

            return mapToResponse(saved);
        } catch (IOException e) {
            log.error("Failed to upload file", e);
            throw new RuntimeException("Failed to upload file: " + e.getMessage());
        }
    }

    @Override
    @Transactional
    public List<FileResponse> uploadMultipleFiles(List<MultipartFile> files, UUID userId,
            UUID folderId, String[] tags, String description) {
        return files.stream()
                .map(file -> uploadFile(file, userId, folderId, tags, description))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public FileResponse getFileById(UUID userId, UUID fileId) {
        FileEntity file = fileRepository.findByIdAndUserIdAndIsActive(fileId, userId, "Y")
                .orElseThrow(() -> new RuntimeException("File not found"));
        return mapToResponse(file);
    }

    @Override
    @Transactional(readOnly = true)
    public List<FileResponse> getFilesByFolder(UUID userId, UUID folderId) {
        List<FileEntity> files = fileRepository.findByUserIdAndFolderIdAndIsActive(userId, folderId, "Y");
        return files.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public Page<FileResponse> getFilesByFolderPaginated(UUID userId, UUID folderId, Pageable pageable) {
        Page<FileEntity> files = fileRepository.findByUserIdAndFolderIdAndIsActive(userId, folderId, "Y", pageable);
        return files.map(this::mapToResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<FileResponse> getFilesByUser(UUID userId, Pageable pageable) {
        Page<FileEntity> files = fileRepository.findByUserIdAndIsActive(userId, "Y", pageable);
        return files.map(this::mapToResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<FileResponse> getFilesByType(UUID userId, FileTypeEnum fileType, Pageable pageable) {
        Page<FileEntity> files = fileRepository.findByUserIdAndFileTypeAndIsActive(userId, fileType, "Y", pageable);
        return files.map(this::mapToResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<FileResponse> searchFiles(UUID userId, String keyword, Pageable pageable) {
        Page<FileEntity> files = fileRepository.searchByKeyword(userId, keyword, "Y", pageable);
        return files.map(this::mapToResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public List<FileResponse> getFilesByTag(UUID userId, String tag) {
        List<FileEntity> files = fileRepository.findByUserIdAndTag(userId, tag);
        return files.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public FileResponse updateFile(UUID userId, UUID fileId, UUID folderId,
            String[] tags, String description) {
        FileEntity file = fileRepository.findByIdAndUserIdAndIsActive(fileId, userId, "Y")
                .orElseThrow(() -> new RuntimeException("File not found"));

        if (folderId != null) {
            folderRepository.findByIdAndUserIdAndIsActive(folderId, userId, "Y")
                    .orElseThrow(() -> new RuntimeException("Folder not found"));
            file.setFolderId(folderId);
        }

        if (tags != null) {
            file.setTags(tags);
        }

        if (description != null) {
            file.setDescription(description);
        }

        file.setUpdatedBy(userId);
        FileEntity updated = fileRepository.save(file);
        return mapToResponse(updated);
    }

    @Override
    @Transactional
    public void deleteFile(UUID userId, UUID fileId) {
        FileEntity file = fileRepository.findByIdAndUserIdAndIsActive(fileId, userId, "Y")
                .orElseThrow(() -> new RuntimeException("File not found"));

        // Check if file is being used
        long usageCount = usageRepository.countUsagesByFileId(fileId);
        if (usageCount > 0) {
            throw new RuntimeException("Cannot delete file that is currently in use");
        }

        try {
            objectStorageService.delete(file.getCloudinaryPublicId());
            if (file.getThumbnailObjectKey() != null
                    && !file.getThumbnailObjectKey().equals(file.getCloudinaryPublicId())) {
                objectStorageService.delete(file.getThumbnailObjectKey());
            }

            // Soft delete from database
            file.setIsActive("N");
            file.setUpdatedBy(userId);
            fileRepository.save(file);

            log.info("File deleted successfully: {}", fileId);
        } catch (RuntimeException e) {
            log.error("Failed to delete file from MinIO", e);
            throw new RuntimeException("Failed to delete file: " + e.getMessage());
        }
    }

    @Override
    @Transactional
    public void updateFileUsage(UUID fileId) {
        // This method is no longer needed as usage is tracked via FileUsageService
        log.debug("updateFileUsage called for file: {}", fileId);
    }

    @Override
    @Transactional(readOnly = true)
    public FileStatisticsResponse getFileStatistics(UUID userId) {
        Long totalFiles = fileRepository.countByUserId(userId, "Y");
        Long totalSize = fileRepository.getTotalFileSizeByUserId(userId, "Y");

        List<Object[]> typeStats = fileRepository.getFileStatisticsByUserId(userId, "Y");
        Map<FileTypeEnum, FileStatisticsResponse.TypeStatistics> byType = new HashMap<>();

        for (Object[] stat : typeStats) {
            FileTypeEnum type = (FileTypeEnum) stat[0];
            Long count = (Long) stat[1];
            Long size = (Long) stat[2];

            byType.put(type, FileStatisticsResponse.TypeStatistics.builder()
                    .count(count)
                    .totalSize(size)
                    .build());
        }

        return FileStatisticsResponse.builder()
                .totalFiles(totalFiles)
                .totalSize(totalSize != null ? totalSize : 0L)
                .byType(byType)
                .build();
    }

    private FileTypeEnum determineFileType(String mimeType) {
        if (mimeType == null) {
            return FileTypeEnum.OTHER;
        }

        if (mimeType.startsWith("image/")) {
            return FileTypeEnum.IMAGE;
        } else if (mimeType.startsWith("video/")) {
            return FileTypeEnum.VIDEO;
        } else if (mimeType.startsWith("audio/")) {
            return FileTypeEnum.AUDIO;
        } else if (mimeType.contains("pdf") || mimeType.contains("document") ||
                mimeType.contains("text") || mimeType.contains("sheet")) {
            return FileTypeEnum.DOCUMENT;
        } else {
            return FileTypeEnum.OTHER;
        }
    }

    private String buildObjectKey(UUID userId, FileTypeEnum fileType, String originalFilename) {
        String safeFilename = Optional.ofNullable(originalFilename)
                .map(name -> name.replaceAll("[^a-zA-Z0-9._-]", "_"))
                .filter(name -> !name.isBlank())
                .orElse("file");
        return String.format(
                "users/%s/%s/%s-%s",
                userId,
                fileType.name().toLowerCase(Locale.ROOT),
                UUID.randomUUID(),
                safeFilename);
    }

    private void populateImageMetadata(MultipartFile file, FileTypeEnum fileType, FileEntity fileEntity)
            throws IOException {
        if (fileType != FileTypeEnum.IMAGE) {
            return;
        }

        BufferedImage image = ImageIO.read(file.getInputStream());
        if (image == null) {
            return;
        }

        fileEntity.setWidth(image.getWidth());
        fileEntity.setHeight(image.getHeight());
    }

    private void initializeProcessingState(FileEntity fileEntity) {
        if (fileEntity.getFileType() == FileTypeEnum.VIDEO) {
            fileEntity.setProcessingStatus("PENDING");
            return;
        }

        fileEntity.setProcessingStatus("READY");
        fileEntity.setProcessedAt(LocalDateTime.now());
        if (fileEntity.getFileType() == FileTypeEnum.IMAGE) {
            fileEntity.setThumbnailObjectKey(fileEntity.getObjectKey());
            fileEntity.setThumbnailUrl(fileEntity.getPublicUrl());
        }
    }

    private void publishProcessingEventIfNeeded(FileEntity saved) {
        if (saved.getFileType() != FileTypeEnum.VIDEO) {
            return;
        }

        fileEventPublisher.publishFileUploaded(
                FileUploadedEvent.builder()
                        .fileId(saved.getId())
                        .userId(saved.getUserId())
                        .bucketName(saved.getBucketName())
                        .objectKey(saved.getObjectKey())
                        .fileType(saved.getFileType().name())
                        .mimeType(saved.getMimeType())
                        .publicUrl(saved.getPublicUrl())
                        .build());
    }

    private FileResponse mapToResponse(FileEntity file) {
        String folderName = null;
        if (file.getFolderId() != null) {
            folderName = folderRepository.findById(file.getFolderId())
                    .map(FileFolderEntity::getName)
                    .orElse(null);
        }

        return FileResponse.builder()
                .id(file.getId())
                .userId(file.getUserId())
                .folderId(file.getFolderId())
                .folderName(folderName)
                .name(file.getName())
                .originalName(file.getOriginalName())
                .fileType(file.getFileType())
                .mimeType(file.getMimeType())
                .fileSize(file.getFileSize())
                .cloudinaryPublicId(file.getCloudinaryPublicId())
                .cloudinaryUrl(file.getCloudinaryUrl())
                .cloudinarySecureUrl(file.getCloudinarySecureUrl())
                .storageProvider(file.getStorageProvider())
                .bucketName(file.getBucketName())
                .objectKey(file.getObjectKey())
                .publicUrl(file.getPublicUrl())
                .secureUrl(file.getSecureUrl())
                .thumbnailObjectKey(file.getThumbnailObjectKey())
                .thumbnailUrl(file.getThumbnailUrl())
                .processingStatus(file.getProcessingStatus())
                .processingError(file.getProcessingError())
                .processedAt(file.getProcessedAt())
                .width(file.getWidth())
                .height(file.getHeight())
                .duration(file.getDuration())
                .tags(file.getTags())
                .altText(file.getAltText())
                .caption(file.getCaption())
                .description(file.getDescription())
                .uploadSource(file.getUploadSource())
                .referenceId(file.getReferenceId())
                .referenceType(file.getReferenceType())
                .created(file.getCreated())
                .updated(file.getUpdated())
                .build();
    }
}
