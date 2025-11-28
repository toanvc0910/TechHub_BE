package com.techhub.app.blogservice.service.impl;

import com.techhub.app.blogservice.dto.request.CommentRequest;
import com.techhub.app.blogservice.dto.response.CommentResponse;
import com.techhub.app.blogservice.entity.Blog;
import com.techhub.app.blogservice.entity.BlogComment;
import com.techhub.app.blogservice.enums.BlogStatus;
import com.techhub.app.blogservice.enums.CommentTargetType;
import com.techhub.app.blogservice.repository.BlogCommentRepository;
import com.techhub.app.blogservice.repository.BlogRepository;
import com.techhub.app.blogservice.service.BlogCommentService;
import com.techhub.app.commonservice.context.UserContext;
import com.techhub.app.commonservice.exception.ForbiddenException;
import com.techhub.app.commonservice.exception.NotFoundException;
import com.techhub.app.commonservice.exception.UnauthorizedException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class BlogCommentServiceImpl implements BlogCommentService {

    private static final String ROLE_ADMIN = "ADMIN";
    private static final String ROLE_INSTRUCTOR = "INSTRUCTOR";

    private final BlogRepository blogRepository;
    private final BlogCommentRepository blogCommentRepository;
    private final SimpMessagingTemplate messagingTemplate;

    @Override
    @Transactional(readOnly = true)
    public List<CommentResponse> getComments(UUID blogId) {
        Blog blog = resolveReadableBlog(blogId);

        List<BlogComment> comments = blogCommentRepository
                .findByTargetIdAndTargetTypeAndIsActiveTrueOrderByCreatedAsc(blog.getId(), CommentTargetType.BLOG);

        return buildCommentTree(comments);
    }

    @Override
    public CommentResponse addComment(UUID blogId, CommentRequest request) {
        UUID currentUserId = requireUser();
        Blog blog = resolveReadableBlog(blogId);

        BlogComment comment = new BlogComment();
        comment.setContent(request.getContent().trim());
        comment.setUserId(currentUserId);
        comment.setTargetId(blog.getId());
        comment.setTargetType(CommentTargetType.BLOG);
        comment.setParentId(resolveParentId(blog.getId(), request.getParentId()));
        comment.setCreatedBy(currentUserId);
        comment.setUpdatedBy(currentUserId);
        comment.setCreated(OffsetDateTime.now());
        comment.setUpdated(OffsetDateTime.now());

        BlogComment saved = blogCommentRepository.save(comment);
        log.info("Comment {} created on blog {} by {}", saved.getId(), blogId, currentUserId);
        
        // Build response
        CommentResponse response = toResponse(saved, new ArrayList<>());
        
        // Broadcast to all subscribers via WebSocket
        String destination = "/topic/blog/" + blogId + "/comments";
        log.info(">>> Broadcasting new comment to WebSocket: {}", destination);
        messagingTemplate.convertAndSend(destination, response);
        
        return response;
    }

    @Override
    public void deleteComment(UUID blogId, UUID commentId) {
        UUID currentUserId = requireUser();
        Blog blog = resolveReadableBlog(blogId);

        BlogComment comment = blogCommentRepository.findByIdAndTargetIdAndIsActiveTrue(commentId, blog.getId())
                .orElseThrow(() -> new NotFoundException("Comment not found"));

        if (!canDeleteComment(comment, blog, currentUserId)) {
            throw new ForbiddenException("You are not allowed to delete this comment");
        }

        comment.setIsActive(false);
        comment.setUpdatedBy(currentUserId);
        comment.setUpdated(OffsetDateTime.now());
        blogCommentRepository.save(comment);
        log.info("Comment {} soft deleted by {}", commentId, currentUserId);
    }

    private Blog resolveReadableBlog(UUID blogId) {
        Blog blog = blogRepository.findByIdAndIsActiveTrue(blogId)
                .orElseThrow(() -> new NotFoundException("Blog not found"));

        if (blog.getStatus() != BlogStatus.PUBLISHED && !isAuthorOrPrivileged(blog.getAuthorId())) {
            throw new ForbiddenException("You are not allowed to access this blog");
        }
        return blog;
    }

    private UUID resolveParentId(UUID blogId, UUID parentId) {
        if (parentId == null) {
            return null;
        }
        return blogCommentRepository.findByIdAndTargetIdAndIsActiveTrue(parentId, blogId)
                .map(BlogComment::getId)
                .orElseThrow(() -> new NotFoundException("Parent comment not found"));
    }

    private List<CommentResponse> buildCommentTree(List<BlogComment> comments) {
        Map<UUID, CommentResponse> responseMap = new LinkedHashMap<>();

        // Pre-create nodes
        for (BlogComment comment : comments) {
            responseMap.put(comment.getId(), toResponse(comment, new ArrayList<>()));
        }

        List<CommentResponse> roots = new ArrayList<>();

        for (BlogComment comment : comments) {
            CommentResponse current = responseMap.get(comment.getId());
            if (comment.getParentId() == null) {
                roots.add(current);
            } else {
                CommentResponse parent = responseMap.get(comment.getParentId());
                if (parent != null) {
                    parent.getReplies().add(current);
                } else {
                    roots.add(current);
                }
            }
        }

        roots.forEach(this::sortRepliesRecursively);
        return roots;
    }

    private CommentResponse sortRepliesRecursively(CommentResponse response) {
        response.getReplies().sort(Comparator.comparing(CommentResponse::getCreated));
        response.getReplies().forEach(this::sortRepliesRecursively);
        return response;
    }

    private CommentResponse toResponse(BlogComment comment, List<CommentResponse> replies) {
        return CommentResponse.builder()
                .id(comment.getId())
                .content(comment.getContent())
                .userId(comment.getUserId())
                .parentId(comment.getParentId())
                .created(comment.getCreated())
                .replies(replies)
                .build();
    }

    private boolean canDeleteComment(BlogComment comment, Blog blog, UUID currentUserId) {
        if (currentUserId.equals(comment.getUserId())) {
            return true;
        }
        if (currentUserId.equals(blog.getAuthorId())) {
            return true;
        }
        return UserContext.hasAnyRole(ROLE_ADMIN, ROLE_INSTRUCTOR);
    }

    private boolean isAuthorOrPrivileged(UUID authorId) {
        UUID currentUserId = UserContext.getCurrentUserId();
        if (currentUserId == null) {
            return false;
        }
        return currentUserId.equals(authorId) || UserContext.hasAnyRole(ROLE_ADMIN, ROLE_INSTRUCTOR);
    }

    private UUID requireUser() {
        UUID userId = UserContext.getCurrentUserId();
        if (userId == null) {
            throw new UnauthorizedException("Authentication required");
        }
        return userId;
    }
}
