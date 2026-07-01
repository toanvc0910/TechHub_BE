package com.techhub.app.userservice.dto.request;

import lombok.Data;

import javax.validation.constraints.NotNull;
import java.util.List;
import java.util.UUID;

@Data
public class CreateInstructorApplicationRequest {
    @NotNull
    private UUID cvFileId;
    private String cvFileUrl;

    private UUID cccdFrontFileId;
    private String cccdFrontFileUrl;
    private UUID cccdBackFileId;
    private String cccdBackFileUrl;

    private List<CertificateItem> certificates;

    @Data
    public static class CertificateItem {
        @NotNull
        private UUID fileId;
        private String fileUrl;
    }
}
