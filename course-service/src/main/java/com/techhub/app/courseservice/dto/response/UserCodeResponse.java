package com.techhub.app.courseservice.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.OffsetDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserCodeResponse {

    private UUID lessonId;
    private UUID userId;
    private String language;
    private String code;
    private OffsetDateTime savedAt;
}
