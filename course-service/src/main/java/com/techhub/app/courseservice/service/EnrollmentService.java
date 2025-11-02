package com.techhub.app.courseservice.service;

import com.techhub.app.courseservice.dto.CreateEnrollmentDTO;
import com.techhub.app.courseservice.dto.EnrollmentDTO;
import com.techhub.app.courseservice.exception.ResourceNotFoundException;
import com.techhub.app.courseservice.model.Course;
import com.techhub.app.courseservice.model.Enrollment;
import com.techhub.app.courseservice.repository.CourseRepository;
import com.techhub.app.courseservice.repository.EnrollmentRepository;
import com.techhub.app.courseservice.utils.MapperUtil;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class EnrollmentService {

    private final EnrollmentRepository enrollmentRepository;
    private final CourseRepository courseRepository;
    private final MapperUtil mapperUtil;

    public EnrollmentService(EnrollmentRepository enrollmentRepository, CourseRepository courseRepository, MapperUtil mapperUtil) {
        this.enrollmentRepository = enrollmentRepository;
        this.courseRepository = courseRepository;
        this.mapperUtil = mapperUtil;
    }

    @Transactional
    public EnrollmentDTO createEnrollment(CreateEnrollmentDTO createEnrollmentDTO) {
        Course course = courseRepository.findById(createEnrollmentDTO.getCourseId())
                .orElseThrow(() -> new ResourceNotFoundException("Course", "id", createEnrollmentDTO.getCourseId()));

        // Check if enrollment already exists
        enrollmentRepository.findByUserIdAndCourseId(
                createEnrollmentDTO.getUserId(), createEnrollmentDTO.getCourseId())
                .ifPresent(e -> {
                    throw new IllegalArgumentException("User already enrolled in this course");
                });

        Enrollment enrollment = new Enrollment();
        enrollment.setUserId(createEnrollmentDTO.getUserId());
        enrollment.setCourse(course);
        enrollment.setStatus(createEnrollmentDTO.getStatus());
        enrollment.setEnrolledAt(LocalDateTime.now());
        enrollment.setIsActive(true);

        Enrollment savedEnrollment = enrollmentRepository.save(enrollment);
        EnrollmentDTO dto = mapperUtil.map(savedEnrollment, EnrollmentDTO.class);
        dto.setCourseId(course.getId());
        return dto;
    }

    public EnrollmentDTO getEnrollmentById(UUID id) {
        Enrollment enrollment = enrollmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Enrollment", "id", id));
        EnrollmentDTO dto = mapperUtil.map(enrollment, EnrollmentDTO.class);
        dto.setCourseId(enrollment.getCourse().getId());
        return dto;
    }

    public List<EnrollmentDTO> getEnrollmentsByUser(UUID userId) {
        List<Enrollment> enrollments = enrollmentRepository.findByUserId(userId);
        return mapperUtil.mapList(enrollments, EnrollmentDTO.class);
    }

    public List<EnrollmentDTO> getEnrollmentsByCourse(UUID courseId) {
        List<Enrollment> enrollments = enrollmentRepository.findByCourseId(courseId);
        return mapperUtil.mapList(enrollments, EnrollmentDTO.class);
    }

    @Transactional
    public EnrollmentDTO updateEnrollmentStatus(UUID id, String status) {
        Enrollment enrollment = enrollmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Enrollment", "id", id));

        enrollment.setStatus(status);
        if ("COMPLETED".equals(status) && enrollment.getCompletedAt() == null) {
            enrollment.setCompletedAt(LocalDateTime.now());
        }

        Enrollment updatedEnrollment = enrollmentRepository.save(enrollment);
        EnrollmentDTO dto = mapperUtil.map(updatedEnrollment, EnrollmentDTO.class);
        dto.setCourseId(updatedEnrollment.getCourse().getId());
        return dto;
    }

    @Transactional
    public void deleteEnrollment(UUID id) {
        Enrollment enrollment = enrollmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Enrollment", "id", id));
        enrollment.setIsActive(false);
        enrollmentRepository.save(enrollment);
    }
}

