package com.techhub.app.userservice.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
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

    private UUID cccdFrontFileId;
    private String cccdFrontFileUrl;
    private String cccdFrontStatus;
    private String cccdFrontData;
    private String cccdFrontError;

    private UUID cccdBackFileId;
    private String cccdBackFileUrl;
    private String cccdBackStatus;
    private String cccdBackData;
    private String cccdBackError;

    private List<CertificateResponse> certificates;

    private String adminStatus;
    private String adminNote;
    private UUID reviewedBy;
    private LocalDateTime reviewedAt;
    private LocalDateTime created;
    private LocalDateTime updated;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CertificateResponse {
        private UUID id;
        private UUID fileId;
        private String fileUrl;
        private String aiStatus;
        private String aiData;
        private String aiError;
    }
}
