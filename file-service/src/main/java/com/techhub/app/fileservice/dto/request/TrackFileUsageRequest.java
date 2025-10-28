package com.techhub.app.fileservice.dto.request;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TrackFileUsageRequest {

    @NotNull(message = "File ID is required")
    private UUID fileId;

    @NotBlank(message = "Used in type is required")
    private String usedInType;

    @NotNull(message = "Used in ID is required")
    private UUID usedInId;

    private String fieldName;

    private UUID createdBy;
}
