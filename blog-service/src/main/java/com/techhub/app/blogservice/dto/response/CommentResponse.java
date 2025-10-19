package com.techhub.app.blogservice.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CommentResponse {
    private UUID id;
    private String content;
    private UUID userId;
    private UUID parentId;
    private OffsetDateTime created;
    @Builder.Default
    private List<CommentResponse> replies = new ArrayList<>();
}
