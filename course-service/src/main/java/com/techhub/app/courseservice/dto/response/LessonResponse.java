package com.techhub.app.courseservice.dto.response;

import com.techhub.app.courseservice.enums.ContentType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LessonResponse {

    private UUID id;
    private String title;
    private String description;
    private Integer orderIndex;
    private ContentType contentType;
    private String content;
    private Boolean mandatory;
    private Boolean isFree;
    private Float completionWeight;
    private Integer estimatedDuration;
    private Boolean workspaceEnabled;
    private List<String> workspaceLanguages;
    private Map<String, Object> workspaceTemplate;
    private String videoUrl;
    private List<String> documentUrls;
    private OffsetDateTime created;
    private OffsetDateTime updated;
    private List<LessonAssetResponse> assets;
    private Float completion;
    private Boolean completed;
    private OffsetDateTime completedAt;
    private OffsetDateTime progressUpdatedAt;
}
