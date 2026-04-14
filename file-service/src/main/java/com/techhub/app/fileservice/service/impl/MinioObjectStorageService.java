package com.techhub.app.fileservice.service.impl;

import com.techhub.app.fileservice.config.MinioProperties;
import com.techhub.app.fileservice.service.ObjectStorageService;
import io.minio.GetPresignedObjectUrlArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;
import io.minio.http.Method;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FilenameUtils;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;

@Service
@RequiredArgsConstructor
@Slf4j
public class MinioObjectStorageService implements ObjectStorageService {

    private static final int MIN_EXPIRY_SECONDS = 1;
    private static final int MAX_EXPIRY_SECONDS = 7 * 24 * 60 * 60;

    private final MinioClient minioClient;
    private final MinioProperties minioProperties;

    @Override
    public StoredObjectDetails upload(MultipartFile file, String objectKey) {
        try (InputStream inputStream = file.getInputStream()) {
            return upload(inputStream, file.getSize(), file.getContentType(), objectKey);
        } catch (Exception e) {
            log.error("Failed to upload object {} to MinIO", objectKey, e);
            throw new RuntimeException("Failed to upload file to object storage", e);
        }
    }

    @Override
    public StoredObjectDetails upload(InputStream inputStream, long size, String contentType, String objectKey) {
        try {
            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(minioProperties.getBucket())
                            .object(objectKey)
                            .stream(inputStream, size, -1)
                            .contentType(contentType)
                            .build());

            return StoredObjectDetails.builder()
                    .bucket(minioProperties.getBucket())
                    .objectKey(objectKey)
                    .publicUrl(buildPublicUrl(objectKey))
                    .build();
        } catch (Exception e) {
            log.error("Failed to upload object {} to MinIO", objectKey, e);
            throw new RuntimeException("Failed to upload file to object storage", e);
        }
    }

    @Override
    public InputStream getObject(String objectKey) {
        try {
            return minioClient.getObject(
                    io.minio.GetObjectArgs.builder()
                            .bucket(minioProperties.getBucket())
                            .object(objectKey)
                            .build());
        } catch (Exception e) {
            log.error("Failed to get object {} from MinIO", objectKey, e);
            throw new RuntimeException("Failed to read file from object storage", e);
        }
    }

    @Override
    public String getPresignedGetUrl(String objectKey) {
        if (objectKey == null || objectKey.isBlank()) {
            return null;
        }

        int expirySeconds = Math.max(
                MIN_EXPIRY_SECONDS,
                Math.min(minioProperties.getPresignedExpirySeconds(), MAX_EXPIRY_SECONDS));

        try {
            return buildPresignClient().getPresignedObjectUrl(
                    GetPresignedObjectUrlArgs.builder()
                            .method(Method.GET)
                            .bucket(minioProperties.getBucket())
                            .object(objectKey)
                            .expiry(expirySeconds)
                            .build());
        } catch (Exception e) {
            log.warn("Failed to generate presigned URL for object {}, fallback to public URL", objectKey, e);
            return buildPublicUrl(objectKey);
        }
    }

    @Override
    public void delete(String objectKey) {
        try {
            minioClient.removeObject(
                    RemoveObjectArgs.builder()
                            .bucket(minioProperties.getBucket())
                            .object(objectKey)
                            .build());
        } catch (Exception e) {
            log.error("Failed to delete object {} from MinIO", objectKey, e);
            throw new RuntimeException("Failed to delete file from object storage", e);
        }
    }

    private String buildPublicUrl(String objectKey) {
        String normalizedBase = minioProperties.getPublicUrl().replaceAll("/+$", "");
        String normalizedKey = FilenameUtils.separatorsToUnix(objectKey).replaceFirst("^/+", "");
        return normalizedBase + "/" + minioProperties.getBucket() + "/" + normalizedKey;
    }

    private MinioClient buildPresignClient() {
        String presignEndpoint = minioProperties.getPublicUrl();
        if (presignEndpoint == null || presignEndpoint.isBlank()) {
            presignEndpoint = minioProperties.getEndpoint();
        }

        return MinioClient.builder()
                .endpoint(presignEndpoint)
                .credentials(minioProperties.getAccessKey(), minioProperties.getSecretKey())
                .build();
    }
}