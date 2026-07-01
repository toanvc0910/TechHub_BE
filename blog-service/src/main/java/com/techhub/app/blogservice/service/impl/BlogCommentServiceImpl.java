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
import com.techhub.app.commonservice.enums.UserRole;
import com.techhub.app.commonservice.exception.ForbiddenException;
import com.techhub.app.commonservice.exception.NotFoundException;
import com.techhub.app.commonservice.exception.UnauthorizedException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

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

    private static final String ROLE_ADMIN = UserRole.ADMIN.name();
    private static final String ROLE_INSTRUCTOR = UserRole.INSTRUCTOR.name();

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

        String destination = getCommentDestination(blog.getId());
        broadcastAfterCommit(destination, buildCreatedPayload(blog.getId(), saved, response));

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

        String destination = getCommentDestination(blog.getId());
        broadcastAfterCommit(destination, buildDeletedPayload(blog.getId(), comment, currentUserId));
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

    private Map<String, Object> buildCreatedPayload(UUID blogId, BlogComment comment, CommentResponse response) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("eventType", comment.getParentId() == null ? "CREATED" : "REPLIED");
        payload.put("commentId", comment.getId());
        payload.put("id", response.getId());
        payload.put("targetId", blogId);
        payload.put("targetType", "BLOG");
        payload.put("parentId", response.getParentId());
        payload.put("userId", response.getUserId());
        payload.put("content", response.getContent());
        payload.put("createdAt", response.getCreated());
        payload.put("created", response.getCreated());
        payload.put("replies", response.getReplies());
        payload.put("comment", response);
        payload.put("timestamp", OffsetDateTime.now());
        return payload;
    }

    private Map<String, Object> buildDeletedPayload(UUID blogId, BlogComment comment, UUID deletedBy) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("eventType", "DELETED");
        payload.put("commentId", comment.getId());
        payload.put("id", comment.getId());
        payload.put("targetId", blogId);
        payload.put("targetType", "BLOG");
        payload.put("parentId", comment.getParentId());
        payload.put("userId", deletedBy);
        payload.put("timestamp", OffsetDateTime.now());
        return payload;
    }

    private String getCommentDestination(UUID blogId) {
        return "/topic/blog/" + blogId + "/comments";
    }

    private void broadcastAfterCommit(String destination, Map<String, Object> payload) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    broadcast(destination, payload);
                }
            });
            return;
        }

        broadcast(destination, payload);
    }

    private void broadcast(String destination, Map<String, Object> payload) {
        log.info(">>> Broadcasting comment event {} to WebSocket: {}",
                payload.get("eventType"), destination);
        messagingTemplate.convertAndSend(destination, payload);
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
