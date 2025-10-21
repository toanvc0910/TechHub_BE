package com.techhub.app.blogservice.service;

import com.techhub.app.blogservice.dto.request.BlogRequest;
import com.techhub.app.blogservice.dto.response.BlogResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.UUID;

public interface BlogService {

    Page<BlogResponse> getBlogs(String keyword, List<String> tags, boolean includeDrafts, Pageable pageable);

    BlogResponse getBlog(UUID blogId);

    BlogResponse createBlog(BlogRequest request);

    BlogResponse updateBlog(UUID blogId, BlogRequest request);

    void deleteBlog(UUID blogId);

    List<String> getTags();
}
