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
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

            String processingWarning = null;

            try {
                Map<String, String> metadata = extractVideoMetadata(videoFile);
                file.setWidth(parseInteger(metadata.get("width")));
                file.setHeight(parseInteger(metadata.get("height")));
                file.setDuration(parseDuration(metadata.get("duration")));
            } catch (Exception e) {
                processingWarning = appendProcessingWarning(processingWarning,
                        "Video metadata unavailable: " + safeErrorMessage(e));
                log.warn("Video metadata extraction failed for file {}", file.getId(), e);
            }

            try {
                thumbnailFile = Files.createTempFile("techhub-thumbnail-", ".jpg");
                generateVideoThumbnail(videoFile, thumbnailFile);

                String thumbnailObjectKey = buildThumbnailObjectKey(file);
                try (InputStream thumbnailInputStream = Files.newInputStream(thumbnailFile)) {
                    StoredObjectDetails thumbnailObject = objectStorageService.upload(
                            thumbnailInputStream,
                            Files.size(thumbnailFile),
                            "image/jpeg",
                            thumbnailObjectKey);
                    file.setThumbnailObjectKey(thumbnailObject.getObjectKey());
                    file.setThumbnailUrl(thumbnailObject.getPublicUrl());
                }
            } catch (Exception e) {
                processingWarning = appendProcessingWarning(processingWarning,
                        "Video thumbnail unavailable: " + safeErrorMessage(e));
                log.warn("Video thumbnail generation failed for file {}", file.getId(), e);
            }

            file.setProcessingStatus("READY");
            file.setProcessingError(processingWarning);
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
        List<String> command = List.of(
                "ffprobe",
                "-v", "error",
                "-select_streams", "v:0",
                "-show_entries", "stream=width,height:format=duration",
                "-of", "default=noprint_wrappers=1",
                videoFile.toAbsolutePath().toString());

        String output = runProcess(command, "ffprobe");
        Map<String, String> result = new HashMap<>();

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                new java.io.ByteArrayInputStream(output.getBytes(StandardCharsets.UTF_8)),
                StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split("=", 2);
                if (parts.length == 2) {
                    result.put(parts[0], parts[1]);
                }
            }
        }

        return result;
    }

    private void generateVideoThumbnail(Path videoFile, Path thumbnailFile) throws IOException, InterruptedException {
        String videoPath = videoFile.toAbsolutePath().toString();
        String thumbnailPath = thumbnailFile.toAbsolutePath().toString();
        List<List<String>> attempts = List.of(
                List.of("ffmpeg", "-y", "-ss", "00:00:00.5", "-i", videoPath,
                        "-frames:v", "1", "-q:v", "2", thumbnailPath),
                List.of("ffmpeg", "-y", "-i", videoPath,
                        "-frames:v", "1", "-q:v", "2", thumbnailPath));

        Exception lastError = null;
        for (List<String> command : attempts) {
            Files.deleteIfExists(thumbnailFile);
            try {
                runProcess(command, "ffmpeg");
                if (Files.exists(thumbnailFile) && Files.size(thumbnailFile) > 0) {
                    return;
                }
                throw new RuntimeException("ffmpeg did not produce a thumbnail");
            } catch (Exception e) {
                lastError = e;
            }
        }

        if (lastError instanceof IOException ioException) {
            throw ioException;
        }
        if (lastError instanceof InterruptedException interruptedException) {
            throw interruptedException;
        }
        throw new RuntimeException(lastError);
    }

    private String runProcess(List<String> command, String commandName) throws IOException, InterruptedException {
        ProcessBuilder processBuilder = new ProcessBuilder(command);
        processBuilder.redirectErrorStream(true);

        Process process = processBuilder.start();
        StringBuilder output = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (output.length() < 4000) {
                    output.append(line).append(System.lineSeparator());
                }
            }
        }

        int exitCode = process.waitFor();
        if (exitCode != 0) {
            throw new RuntimeException(commandName + " failed with exit code " + exitCode
                    + formatProcessOutput(output.toString()));
        }

        return output.toString();
    }

    private String buildThumbnailObjectKey(FileEntity file) {
        return "users/" + file.getUserId() + "/thumbnails/videos/" + file.getId() + ".jpg";
    }

    private String appendProcessingWarning(String current, String warning) {
        if (current == null || current.isBlank()) {
            return truncate(warning, 1000);
        }
        return truncate(current + "; " + warning, 1000);
    }

    private String safeErrorMessage(Exception e) {
        String message = e.getMessage();
        if (message == null || message.isBlank()) {
            return e.getClass().getSimpleName();
        }
        return truncate(message.replaceAll("\\s+", " ").trim(), 500);
    }

    private String formatProcessOutput(String output) {
        if (output == null || output.isBlank()) {
            return "";
        }
        return ": " + truncate(output.replaceAll("\\s+", " ").trim(), 500);
    }

    private String truncate(String value, int maxLength) {
        if (value == null || value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength);
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
