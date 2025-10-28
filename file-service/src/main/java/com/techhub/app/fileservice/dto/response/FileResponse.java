package com.techhub.app.fileservice.dto.response;

import com.techhub.app.fileservice.enums.FileTypeEnum;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FileResponse {

    private UUID id;
    private UUID userId;
    private UUID folderId;
    private String folderName;
    private String name;
    private String originalName;
    private FileTypeEnum fileType;
    private String mimeType;
    private Long fileSize;
    private String cloudinaryPublicId;
    private String cloudinaryUrl;
    private String cloudinarySecureUrl;
    private Integer width;
    private Integer height;
    private Integer duration;
    private String[] tags;
    private String altText;
    private String caption;
    private String description;
    private String uploadSource;
    private UUID referenceId;
    private String referenceType;
    private LocalDateTime created;
    private LocalDateTime updated;
}
