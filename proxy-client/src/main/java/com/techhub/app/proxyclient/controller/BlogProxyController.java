package com.techhub.app.proxyclient.controller;

import com.techhub.app.proxyclient.client.BlogServiceClient;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/proxy/blogs")
@RequiredArgsConstructor
public class BlogProxyController {

    private final BlogServiceClient blogServiceClient;

    @GetMapping
    public ResponseEntity<String> getAllBlogs(@RequestParam(defaultValue = "0") int page,
                                            @RequestParam(defaultValue = "10") int size,
                                            @RequestParam(required = false) String search) {
        return blogServiceClient.getAllBlogs(page, size, search);
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
}
