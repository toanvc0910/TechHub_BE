package com.techhub.app.userservice.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InstructorApplicationResponse {
    private UUID id;
    private UUID userId;
    private String userName;
    private String userEmail;
    private UUID cvFileId;
    private String cvFileUrl;
    private String aiStatus;
    /** JSON string trả về cho FE parse. */
    private String aiExtractedData;
    private String aiError;
    private String adminStatus;
    private String adminNote;
    private UUID reviewedBy;
    private LocalDateTime reviewedAt;
    private LocalDateTime created;
    private LocalDateTime updated;
}
