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
public class CreateFolderRequest {

    @NotNull(message = "User ID is required")
    private UUID userId;

    private UUID parentId;

    @NotBlank(message = "Folder name is required")
    private String name;

    private String description;
}
