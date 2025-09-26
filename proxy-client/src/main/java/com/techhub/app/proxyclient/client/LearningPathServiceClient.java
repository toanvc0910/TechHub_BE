package com.techhub.app.proxyclient.client;

import com.techhub.app.proxyclient.constant.AppConstant;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@FeignClient(name = "LEARNING-PATH-SERVICE")
public interface LearningPathServiceClient {

    @GetMapping("/api/learning-paths")
    ResponseEntity<String> getAllLearningPaths(@RequestParam(defaultValue = "0") int page,
                                             @RequestParam(defaultValue = "10") int size,
                                             @RequestParam(required = false) String search);

    @PostMapping("/api/learning-paths")
    ResponseEntity<String> createLearningPath(@RequestBody Object createRequest,
                                            @RequestHeader("Authorization") String authHeader);

    @GetMapping("/api/learning-paths/{pathId}")
    ResponseEntity<String> getLearningPathById(@PathVariable String pathId);

    @PutMapping("/api/learning-paths/{pathId}")
    ResponseEntity<String> updateLearningPath(@PathVariable String pathId,
                                            @RequestBody Object updateRequest,
                                            @RequestHeader("Authorization") String authHeader);

    @DeleteMapping("/api/learning-paths/{pathId}")
    ResponseEntity<String> deleteLearningPath(@PathVariable String pathId,
                                            @RequestHeader("Authorization") String authHeader);

    @PostMapping("/api/learning-paths/{pathId}/enroll")
    ResponseEntity<String> enrollLearningPath(@PathVariable String pathId,
                                            @RequestHeader("Authorization") String authHeader);

    @GetMapping("/api/learning-paths/{pathId}/progress")
    ResponseEntity<String> getPathProgress(@PathVariable String pathId,
                                         @RequestHeader("Authorization") String authHeader);
}
