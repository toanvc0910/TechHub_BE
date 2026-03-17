package com.techhub.app.fileservice.kafka;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FileUploadedEvent {

    private UUID fileId;
    private UUID userId;
    private String bucketName;
    private String objectKey;
    private String fileType;
    private String mimeType;
    private String publicUrl;
}