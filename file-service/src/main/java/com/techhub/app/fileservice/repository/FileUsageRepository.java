package com.techhub.app.fileservice.repository;

import com.techhub.app.fileservice.entity.FileUsageEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface FileUsageRepository extends JpaRepository<FileUsageEntity, UUID> {

    // Basic finders (no isActive field in FileUsageEntity)
    List<FileUsageEntity> findByFileId(UUID fileId);

    List<FileUsageEntity> findByUsedInTypeAndUsedInId(String usedInType, UUID usedInId);

    Optional<FileUsageEntity> findByFileIdAndUsedInTypeAndUsedInId(UUID fileId, String usedInType, UUID usedInId);

    boolean existsByFileIdAndUsedInTypeAndUsedInId(UUID fileId, String usedInType, UUID usedInId);

    @Query("SELECT COUNT(fu) FROM FileUsageEntity fu WHERE fu.fileId = :fileId")
    Long countUsagesByFileId(@Param("fileId") UUID fileId);

    void deleteByUsedInTypeAndUsedInId(String usedInType, UUID usedInId);
}
