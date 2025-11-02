package com.techhub.app.courseservice.repository;

import com.techhub.app.courseservice.model.UserCode;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserCodeRepository extends JpaRepository<UserCode, UUID> {
    List<UserCode> findByUserId(UUID userId);
    List<UserCode> findByLessonId(UUID lessonId);
    Optional<UserCode> findByUserIdAndLessonIdAndLanguage(UUID userId, UUID lessonId, String language);
    List<UserCode> findByIsActiveTrue();
}
