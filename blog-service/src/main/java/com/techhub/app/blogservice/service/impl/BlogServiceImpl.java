package com.techhub.app.blogservice.service.impl;

import com.techhub.app.blogservice.dto.request.BlogRequest;
import com.techhub.app.blogservice.dto.response.BlogResponse;
import com.techhub.app.blogservice.entity.Blog;
import com.techhub.app.blogservice.enums.BlogStatus;
import com.techhub.app.blogservice.mapper.BlogMapper;
import com.techhub.app.blogservice.repository.BlogRepository;
import com.techhub.app.blogservice.service.BlogService;
import com.techhub.app.commonservice.context.UserContext;
import com.techhub.app.commonservice.exception.ForbiddenException;
import com.techhub.app.commonservice.exception.NotFoundException;
import com.techhub.app.commonservice.exception.UnauthorizedException;
import com.techhub.app.commonservice.kafka.event.notification.NotificationCommand;
import com.techhub.app.commonservice.kafka.event.notification.NotificationDeliveryMethod;
import com.techhub.app.commonservice.kafka.event.notification.NotificationRecipient;
import com.techhub.app.commonservice.kafka.event.notification.NotificationType;
import com.techhub.app.commonservice.kafka.publisher.NotificationCommandPublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class BlogServiceImpl implements BlogService {

    private static final String ROLE_ADMIN = "ADMIN";
    private static final String ROLE_INSTRUCTOR = "INSTRUCTOR";

    private final BlogRepository blogRepository;
    private final BlogMapper blogMapper;
    private final NotificationCommandPublisher notificationCommandPublisher;

    @Override
    @Transactional(readOnly = true)
    public Page<BlogResponse> getBlogs(String keyword, List<String> tags, boolean includeDrafts, Pageable pageable) {
        String normalizedKeyword = normalize(keyword);
        List<String> normalizedTags = normalizeTags(tags);
        boolean privileged = UserContext.hasAnyRole(ROLE_ADMIN, ROLE_INSTRUCTOR);
        boolean hasFilters = normalizedKeyword != null || !normalizedTags.isEmpty();

        Page<Blog> blogs;
        if (includeDrafts && privileged) {
            blogs = hasFilters
                    ? blogRepository.searchAll(normalizedKeyword, normalizedTags, pageable)
                    : blogRepository.findByIsActiveTrueOrderByCreatedDesc(pageable);
        } else {
            blogs = hasFilters
                    ? blogRepository.searchPublished(BlogStatus.PUBLISHED.name(), normalizedKeyword, normalizedTags,
                            pageable)
                    : blogRepository.findByStatusAndIsActiveTrueOrderByCreatedDesc(BlogStatus.PUBLISHED, pageable);
        }

        return blogs.map(blogMapper::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public BlogResponse getBlog(UUID blogId) {
        Blog blog = blogRepository.findByIdAndIsActiveTrue(blogId)
                .orElseThrow(() -> new NotFoundException("Blog not found"));

        // Nếu blog chưa publish, chỉ author mới xem được
        if (blog.getStatus() != BlogStatus.PUBLISHED) {
            UUID currentUserId = UserContext.getCurrentUserId();
            if (currentUserId == null || !currentUserId.equals(blog.getAuthorId())) {
                throw new NotFoundException("Blog not found");
            }
        }

        return blogMapper.toResponse(blog);
    }

    @Override
    public BlogResponse createBlog(BlogRequest request) {
        log.debug("📝 [CREATE BLOG] ===== START =====");
        log.debug("📝 [CREATE BLOG] Request: title={}, status={}", request.getTitle(), request.getStatus());

        UUID currentUserId = requireUser();
        log.debug("📝 [CREATE BLOG] Current user: {}", currentUserId);

        Blog blog = blogMapper.toEntity(request);
        blog.setAuthorId(currentUserId);
        blog.setCreatedBy(currentUserId);
        blog.setUpdatedBy(currentUserId);

        Blog saved = blogRepository.save(blog);
        log.info("📝 [CREATE BLOG] Blog {} created by {}, status={}", saved.getId(), currentUserId, saved.getStatus());

        log.debug("📝 [CREATE BLOG] Calling notifyPublicationIfNecessary with previousStatus=null");
        notifyPublicationIfNecessary(saved, null);

        log.debug("📝 [CREATE BLOG] ===== END =====");
        return blogMapper.toResponse(saved);
    }

    @Override
    public BlogResponse updateBlog(UUID blogId, BlogRequest request) {
        log.debug("📝 [UPDATE BLOG] ===== START =====");
        log.debug("📝 [UPDATE BLOG] BlogId: {}, Request: title={}, status={}", blogId, request.getTitle(),
                request.getStatus());

        Blog blog = getActiveBlog(blogId);
        ensureCanModify(blog);

        BlogStatus previousStatus = blog.getStatus();
        log.debug("📝 [UPDATE BLOG] Previous status: {}, New status from request: {}", previousStatus,
                request.getStatus());

        blogMapper.applyRequest(blog, request);
        blog.setUpdatedBy(UserContext.getCurrentUserId());

        Blog saved = blogRepository.save(blog);
        log.info("📝 [UPDATE BLOG] Blog {} updated, previousStatus={}, newStatus={}", blogId, previousStatus,
                saved.getStatus());

        log.debug("📝 [UPDATE BLOG] Calling notifyPublicationIfNecessary");
        notifyPublicationIfNecessary(saved, previousStatus);

        log.debug("📝 [UPDATE BLOG] ===== END =====");
        return blogMapper.toResponse(saved);
    }

    @Override
    public void deleteBlog(UUID blogId) {
        Blog blog = getActiveBlog(blogId);
        ensureCanModify(blog);

        blog.setIsActive(false);
        blog.setUpdatedBy(UserContext.getCurrentUserId());
        blogRepository.save(blog);
        log.info("Blog {} soft deleted", blogId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<String> getTags() {
        List<String> tags = blogRepository.findDistinctTags();
        return tags == null ? Collections.emptyList() : tags;
    }

    private Blog getActiveBlog(UUID blogId) {
        return blogRepository.findByIdAndIsActiveTrue(blogId)
                .orElseThrow(() -> new NotFoundException("Blog not found"));
    }

    private void ensureCanModify(Blog blog) {
        // Permission đã được check ở proxy-client
        // Chỉ check ownership nếu cần business logic
        UUID currentUserId = UserContext.getCurrentUserId();
        if (currentUserId == null) {
            throw new ForbiddenException("Authentication required");
        }
        // Cho phép modify nếu là author
        // Nếu không phải author, proxy-client sẽ check permission BLOG_UPDATE/DELETE
    }

    private UUID requireUser() {
        UUID currentUserId = UserContext.getCurrentUserId();
        if (currentUserId == null) {
            throw new UnauthorizedException("Authentication required");
        }
        return currentUserId;
    }

    private String normalize(String keyword) {
        if (keyword == null) {
            return null;
        }
        String trimmed = keyword.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private List<String> normalizeTags(List<String> tags) {
        if (tags == null) {
            return Collections.emptyList();
        }

        return tags.stream()
                .filter(Objects::nonNull)
                .map(String::trim)
                .flatMap(tag -> Arrays.stream(tag.split(",")))
                .map(String::trim)
                .filter(tag -> !tag.isEmpty())
                .map(String::toLowerCase)
                .distinct()
                .collect(Collectors.toList());
    }

    private void notifyPublicationIfNecessary(Blog blog, BlogStatus previousStatus) {
        log.debug("🔔 [NOTIFY] ===== notifyPublicationIfNecessary START =====");
        log.debug("🔔 [NOTIFY] Blog: id={}, status={}, previousStatus={}",
                blog != null ? blog.getId() : "null",
                blog != null ? blog.getStatus() : "null",
                previousStatus);

        if (blog == null || blog.getStatus() != BlogStatus.PUBLISHED) {
            log.debug("🔔 [NOTIFY] SKIP: Blog is null or status is not PUBLISHED (current={})",
                    blog != null ? blog.getStatus() : "null");
            return;
        }
        if (previousStatus == BlogStatus.PUBLISHED) {
            log.debug("🔔 [NOTIFY] SKIP: previousStatus was already PUBLISHED");
            return;
        }

        log.info("🔔 [NOTIFY] ✅ SENDING notification for blog: {} (title={})", blog.getId(), blog.getTitle());

        NotificationRecipient recipient = NotificationRecipient.builder()
                .userId(blog.getAuthorId())
                .build();
        log.debug("🔔 [NOTIFY] Recipient: userId={}", blog.getAuthorId());

        NotificationCommand command = NotificationCommand.builder()
                .type(NotificationType.BLOG)
                .title("Blog published")
                .message(String.format("Your blog \"%s\" is now published.", blog.getTitle()))
                .deliveryMethods(EnumSet.of(NotificationDeliveryMethod.IN_APP))
                .recipients(List.of(recipient))
                .metadata(Map.of(
                        "blogId", blog.getId(),
                        "blogTitle", blog.getTitle(),
                        "authorId", blog.getAuthorId()))
                .build();

        log.debug("🔔 [NOTIFY] NotificationCommand: type={}, title={}, deliveryMethods={}",
                command.getType(), command.getTitle(), command.getDeliveryMethods());

        notificationCommandPublisher.publish(command);
        log.info("🔔 [NOTIFY] ✅ Notification published to Kafka topic");
        log.debug("🔔 [NOTIFY] ===== notifyPublicationIfNecessary END =====");
    }
}
