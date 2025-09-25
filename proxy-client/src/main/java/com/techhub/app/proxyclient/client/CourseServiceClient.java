package com.techhub.app.proxyclient.client;

import com.techhub.app.proxyclient.constant.AppConstant;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@FeignClient(name = "course-service", url = AppConstant.DiscoveredDomainsApi.COURSE_SERVICE_HOST)
public interface CourseServiceClient {

    @GetMapping("/api/courses")
    ResponseEntity<String> getAllCourses(@RequestParam(defaultValue = "0") int page,
                                       @RequestParam(defaultValue = "10") int size,
                                       @RequestParam(required = false) String search);

    @PostMapping("/api/courses")
    ResponseEntity<String> createCourse(@RequestBody Object createRequest,
                                      @RequestHeader("Authorization") String authHeader);

    @GetMapping("/api/courses/{courseId}")
    ResponseEntity<String> getCourseById(@PathVariable String courseId);

    @PutMapping("/api/courses/{courseId}")
    ResponseEntity<String> updateCourse(@PathVariable String courseId,
                                      @RequestBody Object updateRequest,
                                      @RequestHeader("Authorization") String authHeader);

    @DeleteMapping("/api/courses/{courseId}")
    ResponseEntity<String> deleteCourse(@PathVariable String courseId,
                                      @RequestHeader("Authorization") String authHeader);

    // Enrollment endpoints
    @PostMapping("/api/courses/{courseId}/enroll")
    ResponseEntity<String> enrollCourse(@PathVariable String courseId,
                                      @RequestHeader("Authorization") String authHeader);

    @GetMapping("/api/courses/{courseId}/chapters")
    ResponseEntity<String> getCourseChapters(@PathVariable String courseId);
}
