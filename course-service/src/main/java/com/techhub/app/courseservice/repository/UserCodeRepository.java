package com.techhub.app.courseservice.repository;

import com.techhub.app.courseservice.entity.UserCode;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface UserCodeRepository extends JpaRepository<UserCode, UUID> {

    Optional<UserCode> findByUserIdAndLesson_Id(UUID userId, UUID lessonId);
}
    