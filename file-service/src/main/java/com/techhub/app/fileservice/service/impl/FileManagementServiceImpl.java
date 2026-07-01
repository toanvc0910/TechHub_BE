package com.techhub.app.fileservice.service.impl;

import com.techhub.app.fileservice.config.MinioProperties;
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
import com.techhub.app.fileservice.service.MediaProcessingService;
import com.techhub.app.fileservice.service.ObjectStorageService;
import com.techhub.app.fileservice.service.StorageObjectKeyUtils;
import com.techhub.app.fileservice.service.StoredFileContent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.URI;
import java.text.Normalizer;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class FileManagementServiceImpl implements FileManagementService {

    private static final String UPLOAD_SOURCE_AI_CHAT = "AI_CHAT";
    private static final String UPLOAD_SOURCE_DIRECT = "DIRECT";

    private final ObjectStorageService objectStorageService;
    private final MinioProperties minioProperties;
    private final FileRepository fileRepository;
    private final FileFolderRepository folderRepository;
    private final FileUsageRepository usageRepository;
    private final FileEventPublisher fileEventPublisher;
    private final MediaProcessingService mediaProcessingService;

    @Override
    @Transactional
    public FileResponse uploadFile(MultipartFile file, UUID userId, UUID folderId,
            String[] tags, String description, String uploadSource) {
        try {
            FileFolderEntity folder = null;
            if (folderId != null) {
                folder = folderRepository.findByIdAndUserIdAndIsActive(folderId, userId, "Y")
                        .orElseThrow(() -> new RuntimeException("Folder not found"));
            }

            FileTypeEnum fileType = determineFileType(file.getContentType());
            String resolvedUploadSource = resolveUploadSource(uploadSource, description);
            String objectKey = buildObjectKey(userId, fileType, file.getOriginalFilename(), folder,
                    resolvedUploadSource);
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
            fileEntity.setUploadSource(resolvedUploadSource);
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
            UUID folderId, String[] tags, String description, String uploadSource) {
        return files.stream()
                .map(file -> uploadFile(file, userId, folderId, tags, description, uploadSource))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public FileResponse getFileById(UUID userId, UUID fileId) {
        FileEntity file = getActiveFile(userId, fileId);
        return mapToResponse(file);
    }

    @Override
    @Transactional(readOnly = true)
    public StoredFileContent getFileContent(UUID userId, UUID fileId) {
        FileEntity file = getActiveFile(userId, fileId);
        String objectKey = resolveObjectKey(file);
        if (objectKey == null) {
            throw new RuntimeException("File object not found");
        }

        return StoredFileContent.builder()
                .inputStream(objectStorageService.getObject(objectKey))
                .filename(firstNonBlank(file.getOriginalName(), file.getName(), file.getId().toString()))
                .contentType(firstNonBlank(file.getMimeType(), MediaType.APPLICATION_OCTET_STREAM_VALUE))
                .contentLength(file.getFileSize())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public StoredFileContent getFileThumbnail(UUID userId, UUID fileId) {
        FileEntity file = getActiveFile(userId, fileId);
        String thumbnailObjectKey = normalizeObjectKey(
                firstNonBlank(file.getThumbnailObjectKey(), extractObjectPathFromUrl(file.getThumbnailUrl())));
        if (file.getFileType() == FileTypeEnum.VIDEO && thumbnailObjectKey == null) {
            throw new RuntimeException("File thumbnail not found");
        }

        String objectKey = firstNonBlank(thumbnailObjectKey, resolveObjectKey(file));
        if (objectKey == null) {
            throw new RuntimeException("File thumbnail not found");
        }

        String contentType = file.getFileType() == FileTypeEnum.VIDEO && thumbnailObjectKey != null
                ? MediaType.IMAGE_JPEG_VALUE
                : firstNonBlank(file.getMimeType(), MediaType.APPLICATION_OCTET_STREAM_VALUE);

        return StoredFileContent.builder()
                .inputStream(objectStorageService.getObject(objectKey))
                .filename("thumbnail-" + firstNonBlank(file.getOriginalName(), file.getName(), file.getId().toString()))
                .contentType(contentType)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public List<FileResponse> getFilesByFolder(UUID userId, UUID folderId) {
        List<FileEntity> files = fileRepository.findByUserIdAndFolderIdAndIsActive(userId, folderId, "Y");
        return files.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    private FileEntity getActiveFile(UUID userId, UUID fileId) {
        return fileRepository.findByIdAndUserIdAndIsActive(fileId, userId, "Y")
                .orElseThrow(() -> new RuntimeException("File not found"));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<FileResponse> getFilesByFolderPaginated(UUID userId, UUID folderId, Pageable pageable) {
        Page<FileEntity> files = fileRepository.findByUserIdAndFolderIdAndIsActive(userId, folderId, "Y", pageable);
        return files.map(this::mapToResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<FileResponse> searchFilesByFolder(UUID userId, UUID folderId, String keyword, Pageable pageable) {
        Page<FileEntity> files = fileRepository.searchByFolderAndKeyword(userId, folderId, keyword, "Y", pageable);
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

        String objectKey = resolveObjectKey(file);
        deleteStorageObjectAfterCommit(objectKey, fileId, "file");

        String thumbnailObjectKey = normalizeObjectKey(
                firstNonBlank(file.getThumbnailObjectKey(), extractObjectPathFromUrl(file.getThumbnailUrl())));
        if (thumbnailObjectKey != null && !thumbnailObjectKey.equals(objectKey)) {
            deleteStorageObjectAfterCommit(thumbnailObjectKey, fileId, "thumbnail");
        }

        // Soft delete from database. Storage cleanup is best-effort so stale MinIO state does not block users.
        file.setIsActive("N");
        file.setUpdatedBy(userId);
        fileRepository.save(file);

        log.info("File deleted successfully: {}", fileId);
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

    private String buildObjectKey(UUID userId, FileTypeEnum fileType, String originalFilename, FileFolderEntity folder,
            String uploadSource) {
        String safeFilename = Optional.ofNullable(originalFilename)
                .map(name -> name.replaceAll("[^a-zA-Z0-9._-]", "_"))
                .filter(name -> !name.isBlank())
                .orElse("file");
        String basePrefix = buildStorageBasePrefix(userId, folder, uploadSource);
        return String.format(
                "%s/%s/%s-%s",
                basePrefix,
                storageTypeSegment(fileType),
                UUID.randomUUID(),
                safeFilename);
    }

    private String buildStorageBasePrefix(UUID userId, FileFolderEntity folder, String uploadSource) {
        if (UPLOAD_SOURCE_AI_CHAT.equalsIgnoreCase(uploadSource)) {
            return "users/" + userId + "/ai-chat";
        }

        return StorageObjectKeyUtils.buildLibraryPrefix(userId, folder != null ? folder.getPath() : null);
    }

    private String storageTypeSegment(FileTypeEnum fileType) {
        if (fileType == FileTypeEnum.IMAGE) {
            return "images";
        }
        if (fileType == FileTypeEnum.VIDEO) {
            return "videos";
        }
        if (fileType == FileTypeEnum.AUDIO) {
            return "audio";
        }
        if (fileType == FileTypeEnum.DOCUMENT) {
            return "documents";
        }
        return "other";
    }

    private String sanitizeFolderPath(String path) {
        if (path == null || path.isBlank()) {
            return null;
        }
        return Arrays.stream(path.split("/+"))
                .map(this::sanitizePathSegment)
                .filter(segment -> !segment.isBlank())
                .collect(Collectors.joining("/"));
    }

    private String sanitizePathSegment(String value) {
        if (value == null) {
            return "";
        }
        String asciiValue = Normalizer.normalize(value.trim(), Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "")
                .replace('đ', 'd')
                .replace('Đ', 'D');
        return asciiValue
                .replaceAll("[\\\\/]+", "-")
                .replaceAll("[^a-zA-Z0-9._ -]", "_")
                .replaceAll("\\s+", "_")
                .replaceAll("_+", "_")
                .replaceAll("^[_ .-]+|[_ .-]+$", "");
    }

    private String resolveUploadSource(String uploadSource, String description) {
        if (uploadSource != null && !uploadSource.isBlank()) {
            return uploadSource.trim().replaceAll("[^a-zA-Z0-9_-]", "_").toUpperCase(Locale.ROOT);
        }
        if (description != null && description.toLowerCase(Locale.ROOT).contains("ai chat")) {
            return UPLOAD_SOURCE_AI_CHAT;
        }
        return UPLOAD_SOURCE_DIRECT;
    }

    private void deleteStorageObjectAfterCommit(String objectKey, UUID fileId, String objectKind) {
        if (objectKey == null || objectKey.isBlank()) {
            return;
        }
        Runnable cleanup = () -> CompletableFuture.runAsync(() -> {
            try {
                objectStorageService.delete(objectKey);
            } catch (RuntimeException ex) {
                log.warn("Failed to delete {} object {} for file {}; metadata was already deleted", objectKind,
                        objectKey, fileId, ex);
            }
        });

        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    cleanup.run();
                }
            });
            return;
        }

        cleanup.run();
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
        FileUploadedEvent event = FileUploadedEvent.builder()
                .fileId(saved.getId())
                .userId(saved.getUserId())
                .bucketName(saved.getBucketName())
                .objectKey(saved.getObjectKey())
                .fileType(saved.getFileType().name())
                .mimeType(saved.getMimeType())
                .publicUrl(saved.getPublicUrl())
                .secureUrl(saved.getSecureUrl())
                .name(saved.getName())
                .originalName(saved.getOriginalName())
                .build();

        boolean enqueued = fileEventPublisher.publishFileUploaded(event);

        if (enqueued || saved.getFileType() != FileTypeEnum.VIDEO) {
            return;
        }

        log.warn("Kafka enqueue failed for file {}, fallback to background processing", saved.getId());
        runVideoProcessingFallbackAfterCommit(event);
    }

    private void runVideoProcessingFallbackAfterCommit(FileUploadedEvent event) {
        Runnable fallback = () -> CompletableFuture.runAsync(() -> {
            try {
                mediaProcessingService.processUploadedVideo(event);
            } catch (Exception ex) {
                log.error("Background fallback processing failed for file {}", event.getFileId(), ex);
            }
        });

        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    fallback.run();
                }
            });
            return;
        }

        fallback.run();
    }

    private String resolveSignedObjectUrl(FileEntity file) {
        String fallbackUrl = firstNonBlank(
                file.getSecureUrl(),
                file.getCloudinarySecureUrl(),
                file.getPublicUrl(),
                file.getCloudinaryUrl());

        String objectKey = resolveObjectKey(file);
        if (shouldAttemptPresign(file) && objectKey != null) {
            String signedUrl = objectStorageService.getPresignedGetUrl(objectKey);
            return firstNonBlank(signedUrl, fallbackUrl);
        }

        return fallbackUrl;
    }

    private String resolveSignedThumbnailUrl(FileEntity file, String signedObjectUrl) {
        String fallbackThumbnailUrl = file.getFileType() == FileTypeEnum.VIDEO
                ? file.getThumbnailUrl()
                : firstNonBlank(file.getThumbnailUrl(), signedObjectUrl);

        String thumbnailObjectKey = normalizeObjectKey(
                firstNonBlank(file.getThumbnailObjectKey(), extractObjectPathFromUrl(file.getThumbnailUrl())));
        if ((shouldAttemptPresign(file) || isMinioUrl(file.getThumbnailUrl())) && thumbnailObjectKey != null) {
            String signedUrl = objectStorageService.getPresignedGetUrl(thumbnailObjectKey);
            return firstNonBlank(signedUrl, fallbackThumbnailUrl);
        }

        return fallbackThumbnailUrl;
    }

    private boolean isMinioStorageCandidate(FileEntity file) {
        String storageProvider = file.getStorageProvider();
        return storageProvider == null || storageProvider.isBlank() || "MINIO".equalsIgnoreCase(storageProvider);
    }

    private boolean shouldAttemptPresign(FileEntity file) {
        if (isMinioStorageCandidate(file)) {
            return true;
        }

        if (hasMinioLikeObjectKey(file.getObjectKey())
                || hasMinioLikeObjectKey(file.getCloudinaryPublicId())
                || hasMinioLikeObjectKey(file.getThumbnailObjectKey())) {
            return true;
        }

        return isMinioUrl(file.getSecureUrl())
                || isMinioUrl(file.getCloudinarySecureUrl())
                || isMinioUrl(file.getPublicUrl())
                || isMinioUrl(file.getCloudinaryUrl())
                || isMinioUrl(file.getThumbnailUrl());
    }

    private String resolveObjectKey(FileEntity file) {
        return normalizeObjectKey(firstNonBlank(
                file.getObjectKey(),
                file.getCloudinaryPublicId(),
                extractObjectPathFromUrl(file.getSecureUrl()),
                extractObjectPathFromUrl(file.getCloudinarySecureUrl()),
                extractObjectPathFromUrl(file.getPublicUrl()),
                extractObjectPathFromUrl(file.getCloudinaryUrl())));
    }

    private boolean hasMinioLikeObjectKey(String value) {
        String normalized = normalizeObjectKey(value);
        return normalized != null && normalized.startsWith("users/");
    }

    private boolean isMinioUrl(String value) {
        String urlHost = extractHost(value);
        if (urlHost == null) {
            return false;
        }

        String publicHost = extractHost(minioProperties.getPublicUrl());
        String endpointHost = extractHost(minioProperties.getEndpoint());

        return (publicHost != null && publicHost.equalsIgnoreCase(urlHost))
                || (endpointHost != null && endpointHost.equalsIgnoreCase(urlHost));
    }

    private String extractHost(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        try {
            return URI.create(value).getHost();
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    private boolean isAbsoluteUrl(String value) {
        if (value == null || value.isBlank()) {
            return false;
        }

        try {
            URI uri = URI.create(value);
            return uri.getScheme() != null && uri.getHost() != null;
        } catch (IllegalArgumentException ex) {
            return false;
        }
    }

    private String extractObjectPathFromUrl(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        try {
            URI uri = URI.create(value);
            return uri.getPath();
        } catch (IllegalArgumentException ex) {
            return value;
        }
    }

    private String normalizeObjectKey(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        String key = value.trim().replace('\\', '/');
        key = key.replaceFirst("^/+", "");

        String bucket = minioProperties.getBucket();
        if (bucket != null && !bucket.isBlank()) {
            String normalizedBucket = bucket.trim().replace('\\', '/').replaceFirst("^/+", "").replaceAll("/+$", "");
            if (key.startsWith(normalizedBucket + "/")) {
                key = key.substring(normalizedBucket.length() + 1);
            }
        }

        return key.isBlank() ? null : key;
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }

        return null;
    }

    private FileResponse mapToResponse(FileEntity file) {
        String folderName = null;
        if (file.getFolderId() != null) {
            folderName = folderRepository.findById(file.getFolderId())
                    .map(FileFolderEntity::getName)
                    .orElse(null);
        }

        String signedObjectUrl = resolveSignedObjectUrl(file);
        String signedThumbnailUrl = resolveSignedThumbnailUrl(file, signedObjectUrl);

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
                .cloudinaryUrl(firstNonBlank(signedObjectUrl, file.getCloudinaryUrl(), file.getPublicUrl()))
                .cloudinarySecureUrl(firstNonBlank(signedObjectUrl, file.getCloudinarySecureUrl()))
                .storageProvider(file.getStorageProvider())
                .bucketName(file.getBucketName())
                .objectKey(file.getObjectKey())
                .publicUrl(firstNonBlank(signedObjectUrl, file.getPublicUrl()))
                .secureUrl(firstNonBlank(signedObjectUrl, file.getSecureUrl(), file.getPublicUrl()))
                .thumbnailObjectKey(file.getThumbnailObjectKey())
                .thumbnailUrl(signedThumbnailUrl)
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
