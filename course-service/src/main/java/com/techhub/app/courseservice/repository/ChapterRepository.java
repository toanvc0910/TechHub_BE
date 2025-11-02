package com.techhub.app.courseservice.repository;

import com.techhub.app.courseservice.model.Chapter;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ChapterRepository extends JpaRepository<Chapter, UUID> {
    List<Chapter> findByCourseId(UUID courseId);
    List<Chapter> findByCourseIdOrderByOrderAsc(UUID courseId);
    List<Chapter> findByIsActiveTrue();
}

