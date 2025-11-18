package com.techhub.app.learningpathservice.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CompletionPercentageResponse {
    private UUID userId;
    private UUID pathId;
    private Float completionPercentage;
}
