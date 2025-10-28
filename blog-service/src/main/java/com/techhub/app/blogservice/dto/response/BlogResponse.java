package com.techhub.app.blogservice.dto.response;

import com.techhub.app.blogservice.dto.BlogAttachmentDto;
import com.techhub.app.blogservice.enums.BlogStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BlogResponse {
    private UUID id;
    private String title;
    private String content;
    private String thumbnail;
    private BlogStatus status;
    private List<String> tags;
    private List<BlogAttachmentDto> attachments;
    private UUID authorId;
    private OffsetDateTime created;
    private OffsetDateTime updated;
    private Boolean active;
}
