package com.techhub.app.courseservice.service.impl;

import com.techhub.app.courseservice.dto.CourseRequest;
import com.techhub.app.courseservice.dto.CourseResponse;
import com.techhub.app.courseservice.entity.Course;
import com.techhub.app.courseservice.enums.CourseStatus;
import com.techhub.app.courseservice.repository.CourseRepository;
import com.techhub.app.courseservice.service.CourseService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityNotFoundException;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CourseServiceImpl implements CourseService {

    private final CourseRepository courseRepository;

    @Override
    @Transactional
    public CourseResponse create(CourseRequest request) {
        Course entity = new Course();
        apply(entity, request);
        entity = courseRepository.save(entity);
        return toResponse(entity);
    }

    @Override
    @Transactional
    public CourseResponse update(UUID id, CourseRequest request) {
        Course entity = courseRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Course not found"));
        apply(entity, request);
        entity = courseRepository.save(entity);
        return toResponse(entity);
    }

    @Override
    @Transactional(readOnly = true)
    public CourseResponse get(UUID id) {
        Course entity = courseRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Course not found"));
        return toResponse(entity);
    }

    @Override
    @Transactional
    public void delete(UUID id) {
        if (!courseRepository.existsById(id)) {
            throw new EntityNotFoundException("Course not found");
        }
        courseRepository.deleteById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<CourseResponse> list(String search, String category, Pageable pageable) {
        Page<Course> page;
        if (search != null && !search.isBlank()) {
            page = courseRepository.findByTitleContainingIgnoreCase(search, pageable);
        } else {
            page = courseRepository.findAll(pageable);
        }
        return page.map(this::toResponse);
    }

    private void apply(Course entity, CourseRequest request) {
        entity.setTitle(request.getTitle());
        entity.setDescription(request.getDescription());
        entity.setPrice(request.getPrice());
        entity.setInstructorId(request.getInstructorId());
        if (request.getStatus() != null) entity.setStatus(request.getStatus());
        entity.setCategories(request.getCategories());
        entity.setTags(request.getTags());
        entity.setDiscountPrice(request.getDiscountPrice());
        entity.setPromoEndDate(request.getPromoEndDate());
        if (request.getIsActive() != null) entity.setIsActive(request.getIsActive());
    }

    private CourseResponse toResponse(Course e) {
        return CourseResponse.builder()
                .id(e.getId())
                .title(e.getTitle())
                .description(e.getDescription())
                .status(e.getStatus())
                .categories(e.getCategories())
                .tags(e.getTags())
                .price(e.getPrice())
                .discountPrice(e.getDiscountPrice())
                .promoEndDate(e.getPromoEndDate())
                .instructorId(e.getInstructorId())
                .created(e.getCreated())
                .updated(e.getUpdated())
                .createdBy(e.getCreatedBy())
                .updatedBy(e.getUpdatedBy())
                .isActive(e.getIsActive())
                .build();
    }
}
