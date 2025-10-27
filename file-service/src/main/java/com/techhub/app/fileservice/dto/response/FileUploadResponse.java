package com.techhub.app.fileservice.dto.response;

import com.techhub.app.fileservice.entity.FileMetadata;
import com.techhub.app.fileservice.enums.FileType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FileUploadResponse {
    private Long id;
    private String url;
    private String publicId;
    private String originalFilename;
    private FileType fileType;
    private Long size;
    private String format;
    private String folder;
    private LocalDateTime uploadedAt;

    public static FileUploadResponse fromEntity(FileMetadata metadata) {
        return FileUploadResponse.builder()
                .id(metadata.getId())
                .url(metadata.getUrl())
                .publicId(metadata.getPublicId())
                .originalFilename(metadata.getOriginalFilename())
                .fileType(metadata.getFileType())
                .size(metadata.getSize())
                .format(metadata.getFormat())
                .folder(metadata.getFolder())
                .uploadedAt(metadata.getCreatedAt())
                .build();
    }
}
