package com.techhub.app.fileservice.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FolderResponse {

    private UUID id;
    private UUID userId;
    private UUID parentId;
    private String name;
    private String path;
    private String description;
    private Boolean isSystem;
    private Integer fileCount;
    private Long totalSize;
    private List<FolderResponse> children;
    private LocalDateTime created;
    private LocalDateTime updated;
}
