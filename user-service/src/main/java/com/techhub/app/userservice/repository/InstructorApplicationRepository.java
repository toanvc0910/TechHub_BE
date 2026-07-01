package com.techhub.app.userservice.repository;

import com.techhub.app.userservice.entity.InstructorApplication;
import com.techhub.app.userservice.enums.InstructorApplicationAdminStatus;
import com.techhub.app.userservice.enums.InstructorApplicationAiStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
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

    @Modifying
    @Query("UPDATE InstructorApplication a SET a.aiStatus=:s, a.aiExtractedData=:d, a.aiError=:e WHERE a.id=:id")
    void updateCvResult(@Param("id") UUID id, @Param("s") InstructorApplicationAiStatus status,
                        @Param("d") String data, @Param("e") String error);

    @Modifying
    @Query("UPDATE InstructorApplication a SET a.cccdFrontStatus=:s, a.cccdFrontData=:d, a.cccdFrontError=:e WHERE a.id=:id")
    void updateCccdFrontResult(@Param("id") UUID id, @Param("s") InstructorApplicationAiStatus status,
                               @Param("d") String data, @Param("e") String error);

    @Modifying
    @Query("UPDATE InstructorApplication a SET a.cccdBackStatus=:s, a.cccdBackData=:d, a.cccdBackError=:e WHERE a.id=:id")
    void updateCccdBackResult(@Param("id") UUID id, @Param("s") InstructorApplicationAiStatus status,
                              @Param("d") String data, @Param("e") String error);
}
