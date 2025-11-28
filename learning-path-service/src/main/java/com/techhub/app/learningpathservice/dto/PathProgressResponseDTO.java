package com.techhub.app.learningpathservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PathProgressResponseDTO {

    private UUID id;

    private UUID userId;

    private UUID pathId;

    private String pathTitle;

    private Float completion;

    private Map<String, Object> milestones;

    private OffsetDateTime created;

    private OffsetDateTime updated;

    private Boolean isActive;
}
