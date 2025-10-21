package com.techhub.app.blogservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BlogAttachmentDto {
    @NotBlank(message = "Attachment type is required")
    @Pattern(regexp = "^(image|pdf)$", message = "Attachment type must be either 'image' or 'pdf'")
    private String type;

    @NotBlank(message = "Attachment URL is required")
    @Size(max = 2048, message = "Attachment URL is too long")
    private String url;

    @Size(max = 255, message = "Caption cannot exceed 255 characters")
    private String caption;

    @Size(max = 255, message = "Alt text cannot exceed 255 characters")
    private String altText;
}
