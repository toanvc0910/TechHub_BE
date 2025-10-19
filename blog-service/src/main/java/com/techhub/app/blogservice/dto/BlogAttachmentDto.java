package com.techhub.app.blogservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BlogAttachmentDto {
    private String type;
    private String url;
    private String caption;
    private String altText;
}
