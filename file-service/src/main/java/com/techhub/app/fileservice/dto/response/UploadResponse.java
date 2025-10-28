package com.techhub.app.fileservice.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UploadResponse {
    private String publicId;
    private String url;
    private String secureUrl;
    private String format;
    private String resourceType;
    private Integer width;
    private Integer height;
    private Long bytes;
}
