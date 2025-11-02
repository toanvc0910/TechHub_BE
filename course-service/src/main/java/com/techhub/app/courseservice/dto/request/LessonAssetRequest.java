package com.techhub.app.courseservice.dto.request;

import com.techhub.app.courseservice.enums.LessonAssetType;
import lombok.Getter;
import lombok.Setter;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.util.Map;
import java.util.UUID;

@Getter
@Setter
public class LessonAssetRequest {

    @NotNull
    private LessonAssetType assetType;

    private Integer orderIndex;

    @Size(max = 255)
    private String title;

    @Size(max = 2000)
    private String description;

    private UUID fileId;

    private String externalUrl;

    private Map<String, Object> metadata;
}
