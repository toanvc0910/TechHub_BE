package com.techhub.app.proxyclient.controller;

import com.techhub.app.proxyclient.client.CourseServiceClient;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/proxy/courses")
@RequiredArgsConstructor
public class CourseProxyController {

    private final CourseServiceClient courseServiceClient;

    @GetMapping
    public ResponseEntity<String> getAllCourses(@RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String search) {
        return courseServiceClient.getAllCourses(page, size, search);
    }

    @PostMapping
    public ResponseEntity<String> createCourse(@RequestBody Object createRequest,
            @RequestHeader("Authorization") String authHeader) {
        return courseServiceClient.createCourse(createRequest, authHeader);
    }

    @GetMapping("/{courseId}")
    public ResponseEntity<String> getCourseById(@PathVariable String courseId) {
        return courseServiceClient.getCourseById(courseId);
    }

    @PutMapping("/{courseId}")
    public ResponseEntity<String> updateCourse(@PathVariable String courseId,
            @RequestBody Object updateRequest,
            @RequestHeader("Authorization") String authHeader) {
        return courseServiceClient.updateCourse(courseId, updateRequest, authHeader);
    }

    @DeleteMapping("/{courseId}")
    public ResponseEntity<String> deleteCourse(@PathVariable String courseId,
            @RequestHeader("Authorization") String authHeader) {
        return courseServiceClient.deleteCourse(courseId, authHeader);
    }

    // Enrollment endpoints
    @PostMapping("/{courseId}/enroll")
    public ResponseEntity<String> enrollCourse(@PathVariable String courseId,
            @RequestHeader("Authorization") String authHeader) {
        return courseServiceClient.enrollCourse(courseId, authHeader);
    }

    @GetMapping("/{courseId}/chapters")
    public ResponseEntity<String> getCourseChapters(@PathVariable String courseId) {
        return courseServiceClient.getCourseChapters(courseId);
    }
}
