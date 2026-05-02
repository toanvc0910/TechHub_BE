package com.techhub.app.userservice.repository;

import com.techhub.app.userservice.entity.InstructorApplication;
import com.techhub.app.userservice.enums.InstructorApplicationAdminStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface InstructorApplicationRepository extends JpaRepository<InstructorApplication, UUID> {

    List<InstructorApplication> findByUserIdAndIsActiveTrueOrderByCreatedDesc(UUID userId);

    Page<InstructorApplication> findByAdminStatusAndIsActiveTrueOrderByCreatedDesc(
            InstructorApplicationAdminStatus adminStatus, Pageable pageable);

    Page<InstructorApplication> findByIsActiveTrueOrderByCreatedDesc(Pageable pageable);

    Optional<InstructorApplication> findFirstByUserIdAndAdminStatusAndIsActiveTrueOrderByCreatedDesc(
            UUID userId, InstructorApplicationAdminStatus adminStatus);
}
