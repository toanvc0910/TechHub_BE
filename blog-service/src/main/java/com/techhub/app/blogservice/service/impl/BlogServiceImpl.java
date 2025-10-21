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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
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
                    ? blogRepository.searchPublished(BlogStatus.PUBLISHED.name(), normalizedKeyword, normalizedTags, pageable)
                    : blogRepository.findByStatusAndIsActiveTrueOrderByCreatedDesc(BlogStatus.PUBLISHED, pageable);
        }

        return blogs.map(blogMapper::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public BlogResponse getBlog(UUID blogId) {
        Blog blog = blogRepository.findByIdAndIsActiveTrue(blogId)
                .orElseThrow(() -> new NotFoundException("Blog not found"));

        if (blog.getStatus() != BlogStatus.PUBLISHED && !isAuthorOrPrivileged(blog.getAuthorId())) {
            throw new NotFoundException("Blog not found");
        }

        return blogMapper.toResponse(blog);
    }

    @Override
    public BlogResponse createBlog(BlogRequest request) {
        ensureInstructorOrAdmin();
        UUID currentUserId = requireUser();

        Blog blog = blogMapper.toEntity(request);
        blog.setAuthorId(currentUserId);
        blog.setCreatedBy(currentUserId);
        blog.setUpdatedBy(currentUserId);

        Blog saved = blogRepository.save(blog);
        log.info("Blog {} created by {}", saved.getId(), currentUserId);
        return blogMapper.toResponse(saved);
    }

    @Override
    public BlogResponse updateBlog(UUID blogId, BlogRequest request) {
        Blog blog = getActiveBlog(blogId);
        ensureCanModify(blog);

        blogMapper.applyRequest(blog, request);
        blog.setUpdatedBy(UserContext.getCurrentUserId());

        Blog saved = blogRepository.save(blog);
        log.info("Blog {} updated", blogId);
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
        if (!isAuthorOrPrivileged(blog.getAuthorId())) {
            throw new ForbiddenException("You are not allowed to modify this blog");
        }
    }

    private void ensureInstructorOrAdmin() {
        if (!UserContext.hasAnyRole(ROLE_ADMIN, ROLE_INSTRUCTOR)) {
            throw new ForbiddenException("Only instructors or admins can manage blogs");
        }
    }

    private boolean isAuthorOrPrivileged(UUID authorId) {
        UUID currentUserId = UserContext.getCurrentUserId();
        if (currentUserId == null) {
            return false;
        }
        return currentUserId.equals(authorId) || UserContext.hasAnyRole(ROLE_ADMIN, ROLE_INSTRUCTOR);
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
}
