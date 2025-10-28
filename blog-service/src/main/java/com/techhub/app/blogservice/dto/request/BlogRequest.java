package com.techhub.app.blogservice.dto.request;

import com.techhub.app.blogservice.dto.BlogAttachmentDto;
import com.techhub.app.blogservice.enums.BlogStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BlogRequest {

    @NotBlank(message = "Title is required")
    @Size(max = 255, message = "Title cannot exceed 255 characters")
    private String title;

    @NotBlank(message = "Content is required")
    private String content;

    @Size(max = 500, message = "Thumbnail URL cannot exceed 500 characters")
    private String thumbnail;

    private BlogStatus status;

    private List<String> tags;

    @Valid
    private List<BlogAttachmentDto> attachments;
}
