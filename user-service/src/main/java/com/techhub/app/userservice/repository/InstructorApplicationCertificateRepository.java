package com.techhub.app.userservice.repository;

import com.techhub.app.userservice.entity.InstructorApplicationCertificate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface InstructorApplicationCertificateRepository
        extends JpaRepository<InstructorApplicationCertificate, UUID> {

    List<InstructorApplicationCertificate> findByApplicationIdAndIsActiveTrueOrderByCreatedAsc(UUID applicationId);
}
