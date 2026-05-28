package com.techhub.app.userservice.repository;

import com.techhub.app.userservice.entity.InstructorApplicationCertificate;
import com.techhub.app.userservice.enums.InstructorApplicationAiStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface InstructorApplicationCertificateRepository
        extends JpaRepository<InstructorApplicationCertificate, UUID> {

    List<InstructorApplicationCertificate> findByApplicationIdAndIsActiveTrueOrderByCreatedAsc(UUID applicationId);

    @Modifying
    @Query("UPDATE InstructorApplicationCertificate c SET c.aiStatus=:s, c.aiData=:d, c.aiError=:e WHERE c.id=:id")
    void updateScanResult(@Param("id") UUID id, @Param("s") InstructorApplicationAiStatus status,
                          @Param("d") String data, @Param("e") String error);
}
