package com.techhub.app.fileservice.service.impl;

import com.techhub.app.fileservice.entity.FileEntity;
import com.techhub.app.fileservice.enums.FileTypeEnum;
import com.techhub.app.fileservice.kafka.FileUploadedEvent;
import com.techhub.app.fileservice.repository.FileRepository;
import com.techhub.app.fileservice.service.MediaProcessingService;
import com.techhub.app.fileservice.service.ObjectStorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class MediaProcessingServiceImpl implements MediaProcessingService {

    private final FileRepository fileRepository;
    private final ObjectStorageService objectStorageService;

    @Override
    @Transactional
    public void processUploadedVideo(FileUploadedEvent event) {
        FileEntity file = fileRepository.findById(event.getFileId())
                .orElseThrow(() -> new RuntimeException("File not found for processing"));

        if (file.getFileType() != FileTypeEnum.VIDEO || "READY".equals(file.getProcessingStatus())) {
            return;
        }

        Path videoFile = null;
        Path thumbnailFile = null;

        try {
            videoFile = Files.createTempFile("techhub-video-", ".tmp");
            try (InputStream inputStream = objectStorageService.getObject(file.getCloudinaryPublicId())) {
                Files.copy(inputStream, videoFile, StandardCopyOption.REPLACE_EXISTING);
            }

            Map<String, String> metadata = extractVideoMetadata(videoFile);
            file.setWidth(parseInteger(metadata.get("width")));
            file.setHeight(parseInteger(metadata.get("height")));
            file.setDuration(parseDuration(metadata.get("duration")));

            thumbnailFile = Files.createTempFile("techhub-thumbnail-", ".jpg");
            generateVideoThumbnail(videoFile, thumbnailFile);

            String thumbnailObjectKey = buildThumbnailObjectKey(file.getId());
            try (InputStream thumbnailInputStream = Files.newInputStream(thumbnailFile)) {
                StoredObjectDetails thumbnailObject = objectStorageService.upload(
                        thumbnailInputStream,
                        Files.size(thumbnailFile),
                        "image/jpeg",
                        thumbnailObjectKey);
                file.setThumbnailObjectKey(thumbnailObject.getObjectKey());
                file.setThumbnailUrl(thumbnailObject.getPublicUrl());
            }

            file.setProcessingStatus("READY");
            file.setProcessingError(null);
            file.setProcessedAt(LocalDateTime.now());
            fileRepository.save(file);
        } catch (Exception e) {
            file.setProcessingStatus("FAILED");
            file.setProcessingError(e.getMessage());
            fileRepository.save(file);
            log.error("Video processing failed for file {}", file.getId(), e);
        } finally {
            deleteTempFile(videoFile);
            deleteTempFile(thumbnailFile);
        }
    }

    private Map<String, String> extractVideoMetadata(Path videoFile) throws IOException, InterruptedException {
        ProcessBuilder processBuilder = new ProcessBuilder(
                "ffprobe",
                "-v", "error",
                "-select_streams", "v:0",
                "-show_entries", "stream=width,height:format=duration",
                "-of", "default=noprint_wrappers=1",
                videoFile.toAbsolutePath().toString());

        Process process = processBuilder.start();
        Map<String, String> result = new HashMap<>();

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split("=", 2);
                if (parts.length == 2) {
                    result.put(parts[0], parts[1]);
                }
            }
        }

        int exitCode = process.waitFor();
        if (exitCode != 0) {
            throw new RuntimeException("ffprobe failed with exit code " + exitCode);
        }

        return result;
    }

    private void generateVideoThumbnail(Path videoFile, Path thumbnailFile) throws IOException, InterruptedException {
        ProcessBuilder processBuilder = new ProcessBuilder(
                "ffmpeg",
                "-y",
                "-i", videoFile.toAbsolutePath().toString(),
                "-ss", "00:00:01",
                "-vframes", "1",
                thumbnailFile.toAbsolutePath().toString());

        Process process = processBuilder.start();
        int exitCode = process.waitFor();
        if (exitCode != 0) {
            throw new RuntimeException("ffmpeg failed with exit code " + exitCode);
        }
    }

    private String buildThumbnailObjectKey(UUID fileId) {
        return "thumbnails/" + fileId + ".jpg";
    }

    private Integer parseInteger(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return Integer.parseInt(value);
    }

    private Integer parseDuration(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return (int) Math.round(Double.parseDouble(value));
    }

    private void deleteTempFile(Path path) {
        if (path == null) {
            return;
        }
        try {
            Files.deleteIfExists(path);
        } catch (IOException e) {
            log.warn("Failed to delete temp file {}", path, e);
        }
    }
}