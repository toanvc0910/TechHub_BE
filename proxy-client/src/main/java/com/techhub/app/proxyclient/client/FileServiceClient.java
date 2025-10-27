package com.techhub.app.proxyclient.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@FeignClient(name = "FILE-SERVICE")
public interface FileServiceClient {

        @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
        ResponseEntity<Map<String, Object>> uploadFile(
                        @RequestPart("file") MultipartFile file,
                        @RequestParam(value = "folder", defaultValue = "uploads") String folder);

        @PostMapping(value = "/upload/multiple", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
        ResponseEntity<Map<String, Object>> uploadMultipleFiles(
                        @RequestPart("files") List<MultipartFile> files,
                        @RequestParam(value = "folder", defaultValue = "uploads") String folder);

        @DeleteMapping("/delete")
        ResponseEntity<Map<String, Object>> deleteFile(@RequestParam("publicId") String publicId);

        @GetMapping("/{id}")
        ResponseEntity<Map<String, Object>> getFileMetadata(@PathVariable("id") Long id);
}
