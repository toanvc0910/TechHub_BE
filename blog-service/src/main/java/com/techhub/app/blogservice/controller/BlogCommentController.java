package com.techhub.app.blogservice.controller;

import com.techhub.app.blogservice.dto.request.CommentRequest;
import com.techhub.app.blogservice.dto.response.CommentResponse;
import com.techhub.app.blogservice.service.BlogCommentService;
import com.techhub.app.commonservice.payload.GlobalResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/blogs/{blogId}/comments")
@RequiredArgsConstructor
@Slf4j
public class BlogCommentController {

    private final BlogCommentService blogCommentService;

    @GetMapping
    public ResponseEntity<GlobalResponse<List<CommentResponse>>> getComments(@PathVariable UUID blogId,
                                                                             HttpServletRequest request) {
        List<CommentResponse> responses = blogCommentService.getComments(blogId);
        return ResponseEntity.ok(
                GlobalResponse.success("Comments retrieved successfully", responses)
                        .withPath(request.getRequestURI())
        );
    }

    @PostMapping
    public ResponseEntity<GlobalResponse<CommentResponse>> addComment(@PathVariable UUID blogId,
                                                                       @Valid @RequestBody CommentRequest commentRequest,
                                                                       HttpServletRequest request) {
        CommentResponse response = blogCommentService.addComment(blogId, commentRequest);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(GlobalResponse.success("Comment added successfully", response)
                        .withStatus("COMMENT_CREATED")
                        .withPath(request.getRequestURI()));
    }

    @DeleteMapping("/{commentId}")
    public ResponseEntity<GlobalResponse<Void>> deleteComment(@PathVariable UUID blogId,
                                                               @PathVariable UUID commentId,
                                                               HttpServletRequest request) {
        blogCommentService.deleteComment(blogId, commentId);
        return ResponseEntity.ok(
                GlobalResponse.<Void>success("Comment deleted successfully", null)
                        .withStatus("COMMENT_DELETED")
                        .withPath(request.getRequestURI())
        );
    }
}
