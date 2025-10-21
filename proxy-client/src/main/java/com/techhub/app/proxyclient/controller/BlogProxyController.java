package com.techhub.app.proxyclient.controller;

import com.techhub.app.proxyclient.client.BlogServiceClient;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/proxy/blogs")
@RequiredArgsConstructor
public class BlogProxyController {

    private final BlogServiceClient blogServiceClient;

    @GetMapping
    public ResponseEntity<String> getAllBlogs(@RequestParam(defaultValue = "0") int page,
                                              @RequestParam(defaultValue = "10") int size,
                                              @RequestParam(required = false) String keyword,
                                              @RequestParam(required = false) List<String> tags,
                                              @RequestParam(name = "includeDrafts", defaultValue = "false") boolean includeDrafts,
                                              @RequestHeader(value = "Authorization", required = false) String authHeader) {
        List<String> normalizedTags = normalizeTags(tags);
        return blogServiceClient.getAllBlogs(page, size, keyword, normalizedTags, includeDrafts, authHeader);
    }

    @GetMapping("/tags")
    public ResponseEntity<String> getTags() {
        return blogServiceClient.getTags();
    }

    @PostMapping
    public ResponseEntity<String> createBlog(@RequestBody Object createRequest,
                                             @RequestHeader("Authorization") String authHeader) {
        return blogServiceClient.createBlog(createRequest, authHeader);
    }

    @GetMapping("/{blogId}")
    public ResponseEntity<String> getBlogById(@PathVariable String blogId) {
        return blogServiceClient.getBlogById(blogId);
    }

    @PutMapping("/{blogId}")
    public ResponseEntity<String> updateBlog(@PathVariable String blogId,
                                             @RequestBody Object updateRequest,
                                             @RequestHeader("Authorization") String authHeader) {
        return blogServiceClient.updateBlog(blogId, updateRequest, authHeader);
    }

    @DeleteMapping("/{blogId}")
    public ResponseEntity<String> deleteBlog(@PathVariable String blogId,
                                             @RequestHeader("Authorization") String authHeader) {
        return blogServiceClient.deleteBlog(blogId, authHeader);
    }

    @GetMapping("/{blogId}/comments")
    public ResponseEntity<String> getComments(@PathVariable String blogId) {
        return blogServiceClient.getComments(blogId);
    }

    @PostMapping("/{blogId}/comments")
    public ResponseEntity<String> addComment(@PathVariable String blogId,
                                             @RequestBody Object request,
                                             @RequestHeader("Authorization") String authHeader) {
        return blogServiceClient.addComment(blogId, request, authHeader);
    }

    @DeleteMapping("/{blogId}/comments/{commentId}")
    public ResponseEntity<String> deleteComment(@PathVariable String blogId,
                                                @PathVariable String commentId,
                                                @RequestHeader("Authorization") String authHeader) {
        return blogServiceClient.deleteComment(blogId, commentId, authHeader);
    }

    private List<String> normalizeTags(List<String> tags) {
        if (tags == null) {
            return null;
        }

        List<String> flattened = tags.stream()
                .filter(Objects::nonNull)
                .map(String::trim)
                .flatMap(tag -> Arrays.stream(tag.split(",")))
                .map(String::trim)
                .filter(tag -> !tag.isEmpty())
                .distinct()
                .collect(Collectors.toList());

        return flattened.isEmpty() ? null : flattened;
    }
}
