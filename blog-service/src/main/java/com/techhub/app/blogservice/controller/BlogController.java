package com.techhub.app.blogservice.controller;

import com.techhub.app.blogservice.dto.request.BlogRequest;
import com.techhub.app.blogservice.dto.response.BlogResponse;
import com.techhub.app.blogservice.service.BlogService;
import com.techhub.app.commonservice.payload.GlobalResponse;
import com.techhub.app.commonservice.payload.PageGlobalResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/blogs")
@RequiredArgsConstructor
@Slf4j
@Validated
public class BlogController {

    private final BlogService blogService;

    @GetMapping
    public ResponseEntity<PageGlobalResponse<BlogResponse>> getBlogs(@RequestParam(defaultValue = "0") int page,
                                                                     @RequestParam(defaultValue = "10") int size,
                                                                     @RequestParam(required = false) String keyword,
                                                                     @RequestParam(required = false) List<String> tags,
                                                                     @RequestParam(defaultValue = "false") boolean includeDrafts,
                                                                     HttpServletRequest request) {
        Pageable pageable = PageRequest.of(page, size);
        Page<BlogResponse> blogPage = blogService.getBlogs(keyword, tags, includeDrafts, pageable);

        PageGlobalResponse.PaginationInfo paginationInfo = PageGlobalResponse.PaginationInfo.builder()
                .page(blogPage.getNumber())
                .size(blogPage.getSize())
                .totalElements(blogPage.getTotalElements())
                .totalPages(blogPage.getTotalPages())
                .first(blogPage.isFirst())
                .last(blogPage.isLast())
                .hasNext(blogPage.hasNext())
                .hasPrevious(blogPage.hasPrevious())
                .build();

        return ResponseEntity.ok(
                PageGlobalResponse.success("Blogs retrieved successfully", blogPage.getContent(), paginationInfo)
                        .withPath(request.getRequestURI())
        );
    }

    @GetMapping("/tags")
    public ResponseEntity<GlobalResponse<List<String>>> getBlogTags(HttpServletRequest request) {
        List<String> tags = blogService.getTags();
        return ResponseEntity.ok(
                GlobalResponse.success("Blog tags retrieved successfully", tags)
                        .withPath(request.getRequestURI())
        );
    }

    @GetMapping("/{blogId}")
    public ResponseEntity<GlobalResponse<BlogResponse>> getBlog(@PathVariable UUID blogId,
                                                                HttpServletRequest request) {
        BlogResponse response = blogService.getBlog(blogId);
        return ResponseEntity.ok(
                GlobalResponse.success("Blog retrieved successfully", response)
                        .withPath(request.getRequestURI())
        );
    }

    @PostMapping
    public ResponseEntity<GlobalResponse<BlogResponse>> createBlog(@Valid @RequestBody BlogRequest blogRequest,
                                                                   HttpServletRequest request) {
        BlogResponse response = blogService.createBlog(blogRequest);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(GlobalResponse.success("Blog created successfully", response)
                        .withStatus("BLOG_CREATED")
                        .withPath(request.getRequestURI()));
    }

    @PutMapping("/{blogId}")
    public ResponseEntity<GlobalResponse<BlogResponse>> updateBlog(@PathVariable UUID blogId,
                                                                   @Valid @RequestBody BlogRequest blogRequest,
                                                                   HttpServletRequest request) {
        BlogResponse response = blogService.updateBlog(blogId, blogRequest);
        return ResponseEntity.ok(
                GlobalResponse.success("Blog updated successfully", response)
                        .withStatus("BLOG_UPDATED")
                        .withPath(request.getRequestURI())
        );
    }

    @DeleteMapping("/{blogId}")
    public ResponseEntity<GlobalResponse<Void>> deleteBlog(@PathVariable UUID blogId,
                                                            HttpServletRequest request) {
        blogService.deleteBlog(blogId);
        return ResponseEntity.ok(
                GlobalResponse.<Void>success("Blog deleted successfully", null)
                        .withStatus("BLOG_DELETED")
                        .withPath(request.getRequestURI())
        );
    }
}
