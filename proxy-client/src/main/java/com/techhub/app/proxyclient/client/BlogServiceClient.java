package com.techhub.app.proxyclient.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@FeignClient(name = "BLOG-SERVICE")
public interface BlogServiceClient {

    @GetMapping("/api/blogs")
    ResponseEntity<String> getAllBlogs(@RequestParam(defaultValue = "0") int page,
                                       @RequestParam(defaultValue = "10") int size,
                                       @RequestParam(name = "keyword", required = false) String keyword,
                                       @RequestParam(name = "tags", required = false) List<String> tags,
                                       @RequestParam(name = "includeDrafts", defaultValue = "false") boolean includeDrafts,
                                       @RequestHeader(value = "Authorization", required = false) String authHeader);

    @PostMapping("/api/blogs")
    ResponseEntity<String> createBlog(@RequestBody Object createRequest,
                                      @RequestHeader("Authorization") String authHeader);

    @GetMapping("/api/blogs/{blogId}")
    ResponseEntity<String> getBlogById(@PathVariable String blogId);

    @PutMapping("/api/blogs/{blogId}")
    ResponseEntity<String> updateBlog(@PathVariable String blogId,
                                      @RequestBody Object updateRequest,
                                      @RequestHeader("Authorization") String authHeader);

    @DeleteMapping("/api/blogs/{blogId}")
    ResponseEntity<String> deleteBlog(@PathVariable String blogId,
                                      @RequestHeader("Authorization") String authHeader);

    @GetMapping("/api/blogs/{blogId}/comments")
    ResponseEntity<String> getComments(@PathVariable String blogId);

    @PostMapping("/api/blogs/{blogId}/comments")
    ResponseEntity<String> addComment(@PathVariable String blogId,
                                      @RequestBody Object request,
                                      @RequestHeader("Authorization") String authHeader);

    @DeleteMapping("/api/blogs/{blogId}/comments/{commentId}")
    ResponseEntity<String> deleteComment(@PathVariable String blogId,
                                         @PathVariable String commentId,
                                         @RequestHeader("Authorization") String authHeader);

    @GetMapping("/api/blogs/tags")
    ResponseEntity<String> getTags();
}
