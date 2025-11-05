package com.techhub.app.courseservice.dto.request;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.techhub.app.courseservice.enums.ContentType;
import lombok.Getter;
import lombok.Setter;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.util.List;
import java.util.Map;

@Getter
@Setter
public class LessonRequest {

    @NotBlank
    @Size(max = 255)
    private String title;

    private String description;

    private Integer orderIndex;

    @NotNull
    private ContentType contentType;

    private String content;

    private Boolean isFree;

    private Boolean mandatory;

    private Float completionWeight;

    // Accept both 'estimatedDuration' and 'duration' from frontend
    @JsonAlias({ "duration", "estimatedDuration" })
    private Integer estimatedDuration;

    private Boolean workspaceEnabled;
    private List<@Size(max = 50) String> workspaceLanguages;

    private Map<String, Object> workspaceTemplate;

    private String videoUrl;

    private List<String> documentUrls;
}
