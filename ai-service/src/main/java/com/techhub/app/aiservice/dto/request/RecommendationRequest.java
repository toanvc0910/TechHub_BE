package com.techhub.app.aiservice.dto.request;

import com.techhub.app.aiservice.enums.RecommendationMode;
import lombok.Data;

import javax.validation.constraints.NotNull;
import java.util.List;
import java.util.UUID;

@Data
public class RecommendationRequest {

    @NotNull
    private UUID userId;

    @NotNull
    private RecommendationMode mode = RecommendationMode.REALTIME;

    private String language = "vi";
    private List<UUID> excludeCourseIds;
    private List<String> preferredLanguages;
}
