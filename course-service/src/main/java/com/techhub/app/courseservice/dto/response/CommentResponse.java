package com.techhub.app.courseservice.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CommentResponse {

    private UUID id;
    private UUID parentId;
    private UUID userId;
    private String content;
    private OffsetDateTime created;
    private OffsetDateTime updated;
    @Builder.Default
    private List<CommentResponse> replies = new ArrayList<>();

    public void addReply(CommentResponse reply) {
        if (replies == null) {
            replies = new ArrayList<>();
        }
        replies.add(reply);
    }
}
