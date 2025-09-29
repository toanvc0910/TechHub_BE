package com.techhub.app.courseservice.controller;

import com.techhub.app.commonservice.payload.GlobalResponse;
import com.techhub.app.commonservice.payload.PageGlobalResponse;
import com.techhub.app.courseservice.dto.CourseRequest;
import com.techhub.app.courseservice.dto.CourseResponse;
import com.techhub.app.courseservice.service.CourseService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/courses")
@RequiredArgsConstructor
@Validated
public class CourseController {

    private final CourseService courseService;

    @PostMapping
    public ResponseEntity<GlobalResponse<CourseResponse>> create(@Valid @RequestBody CourseRequest request) {
        CourseResponse created = courseService.create(request);
        return ResponseEntity.ok(GlobalResponse.success("Course created", created));
    }

    @PutMapping("/{id}")
    public ResponseEntity<GlobalResponse<CourseResponse>> update(@PathVariable("id") UUID id,
                                                                 @Valid @RequestBody CourseRequest request) {
        CourseResponse updated = courseService.update(id, request);
        return ResponseEntity.ok(GlobalResponse.success("Course updated", updated));
    }

    @GetMapping("/{id}")
    public ResponseEntity<GlobalResponse<CourseResponse>> get(@PathVariable("id") UUID id) {
        CourseResponse found = courseService.get(id);
        return ResponseEntity.ok(GlobalResponse.success(found));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<GlobalResponse<Void>> delete(@PathVariable("id") UUID id) {
        courseService.delete(id);
        return ResponseEntity.ok(GlobalResponse.success("Course deleted", null));
    }

    @GetMapping
    public ResponseEntity<PageGlobalResponse<CourseResponse>> list(
            @RequestParam(value = "search", required = false) String search,
            @RequestParam(value = "category", required = false) String category,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "10") int size,
            @RequestParam(value = "sort", defaultValue = "created,DESC") String sort) {

        Sort sortObj = parseSort(sort);
        Pageable pageable = PageRequest.of(page, size, sortObj);
        Page<CourseResponse> result = courseService.list(search, category, pageable);

        PageGlobalResponse.PaginationInfo info = PageGlobalResponse.PaginationInfo.builder()
                .page(result.getNumber())
                .size(result.getSize())
                .totalElements(result.getTotalElements())
                .totalPages(result.getTotalPages())
                .first(result.isFirst())
                .last(result.isLast())
                .hasNext(result.hasNext())
                .hasPrevious(result.hasPrevious())
                .build();

        List<CourseResponse> data = result.getContent();
        return ResponseEntity.ok(PageGlobalResponse.success("Courses fetched", data, info));
    }

    private Sort parseSort(String sort) {
        if (sort == null || sort.isBlank()) return Sort.unsorted();
        String[] parts = sort.split(",");
        String prop = parts[0];
        Sort.Direction dir = (parts.length > 1 && "DESC".equalsIgnoreCase(parts[1])) ? Sort.Direction.DESC : Sort.Direction.ASC;
        return Sort.by(dir, prop);
    }
}
