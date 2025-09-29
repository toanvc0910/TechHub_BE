package com.techhub.app.courseservice.service;

import com.techhub.app.courseservice.dto.CourseRequest;
import com.techhub.app.courseservice.dto.CourseResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface CourseService {
    CourseResponse create(CourseRequest request);
    CourseResponse update(UUID id, CourseRequest request);
    CourseResponse get(UUID id);
    void delete(UUID id);
    Page<CourseResponse> list(String search, String category, Pageable pageable);
}
