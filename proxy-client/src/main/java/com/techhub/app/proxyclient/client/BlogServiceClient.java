package com.techhub.app.proxyclient.client;

import com.techhub.app.proxyclient.constant.AppConstant;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@FeignClient(name = "blog-service", url = AppConstant.DiscoveredDomainsApi.BLOG_SERVICE_HOST)
public interface BlogServiceClient {

    @GetMapping("/api/blogs")
    ResponseEntity<String> getAllBlogs(@RequestParam(defaultValue = "0") int page,
                                     @RequestParam(defaultValue = "10") int size,
                                     @RequestParam(required = false) String search);

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
}
