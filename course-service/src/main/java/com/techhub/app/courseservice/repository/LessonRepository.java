package com.techhub.app.courseservice.repository;

import com.techhub.app.courseservice.model.Lesson;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface LessonRepository extends JpaRepository<Lesson, UUID> {
    List<Lesson> findByChapterId(UUID chapterId);
    List<Lesson> findByChapterIdOrderByOrderAsc(UUID chapterId);
    List<Lesson> findByIsActiveTrue();
}

