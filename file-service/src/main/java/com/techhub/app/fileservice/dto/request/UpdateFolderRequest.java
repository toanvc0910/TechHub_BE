package com.techhub.app.fileservice.dto.request;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotNull;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdateFolderRequest {

    private String name;

    private String description;

    private UUID parentId;
}
