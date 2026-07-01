package com.techhub.app.courseservice.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Map;
import java.util.UUID;

@FeignClient(name = "FILE-SERVICE")
public interface FileServiceClient {

    @GetMapping("/api/files/{fileId}")
    ResponseEntity<Map<String, Object>> getFile(
            @PathVariable("fileId") UUID fileId,
            @RequestParam("userId") UUID userId);
}
