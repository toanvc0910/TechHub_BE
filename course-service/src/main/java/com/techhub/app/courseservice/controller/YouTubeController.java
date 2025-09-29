package com.techhub.app.courseservice.controller;

import com.google.api.services.youtube.model.Video;
import com.techhub.app.commonservice.payload.GlobalResponse;
import com.techhub.app.courseservice.service.youtube.YouTubeUploadService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

@RestController
@RequestMapping("/api/youtube")
@RequiredArgsConstructor
public class YouTubeController {

    private final YouTubeUploadService uploadService;

    @PostMapping("/upload")
    public ResponseEntity<GlobalResponse<Map<String, Object>>> upload(
            @RequestHeader(name = "X-YouTube-Access-Token") String accessToken,
            @RequestParam("file") MultipartFile file,
            @RequestParam String title,
            @RequestParam(required = false) String description,
            @RequestParam(defaultValue = "private") String privacy
    ) throws Exception {
        Path tmp = Files.createTempFile("yt-", "-" + file.getOriginalFilename());
        file.transferTo(tmp.toFile());
        try {
            Video v = uploadService.uploadWithAccessToken(accessToken, tmp, title, description, privacy);
            Map<String, Object> data = Map.of(
                    "videoId", v.getId(),
                    "url", "https://youtu.be/" + v.getId()
            );
            return ResponseEntity.ok(GlobalResponse.success("YouTube video uploaded", data));
        } finally {
            Files.deleteIfExists(tmp);
        }
    }
}

