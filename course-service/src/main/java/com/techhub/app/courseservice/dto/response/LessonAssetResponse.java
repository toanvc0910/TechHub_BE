package com.techhub.app.courseservice.dto.response;

import com.techhub.app.courseservice.enums.LessonAssetType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LessonAssetResponse {

    private UUID id;
    private LessonAssetType assetType;
    private Integer orderIndex;
    private String title;
    private String description;
    private CourseFileResource file;
    private String externalUrl;
    private Map<String, Object> metadata;
    private OffsetDateTime created;
    private OffsetDateTime updated;
}
