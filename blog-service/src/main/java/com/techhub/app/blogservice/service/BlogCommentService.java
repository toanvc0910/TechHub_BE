package com.techhub.app.blogservice.service;

import com.techhub.app.blogservice.dto.request.CommentRequest;
import com.techhub.app.blogservice.dto.response.CommentResponse;

import java.util.List;
import java.util.UUID;

public interface BlogCommentService {

    List<CommentResponse> getComments(UUID blogId);

    CommentResponse addComment(UUID blogId, CommentRequest request);

    void deleteComment(UUID blogId, UUID commentId);
}
