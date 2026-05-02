package com.techhub.app.userservice.dto.request;

import lombok.Data;

import javax.validation.constraints.NotNull;
import java.util.UUID;

@Data
public class CreateInstructorApplicationRequest {
    @NotNull
    private UUID cvFileId;
    private String cvFileUrl;
}
