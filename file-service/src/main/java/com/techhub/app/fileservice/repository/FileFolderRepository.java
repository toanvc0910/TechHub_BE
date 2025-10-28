package com.techhub.app.fileservice.repository;

import com.techhub.app.fileservice.entity.FileFolderEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface FileFolderRepository extends JpaRepository<FileFolderEntity, UUID> {

    // isActive is String type ("Y"/"N"), not Boolean
    List<FileFolderEntity> findByUserIdAndIsActive(UUID userId, String isActive);

    List<FileFolderEntity> findByUserIdAndParentIdAndIsActive(UUID userId, UUID parentId, String isActive);

    List<FileFolderEntity> findByUserIdAndParentIdIsNullAndIsActive(UUID userId, String isActive);

    Optional<FileFolderEntity> findByIdAndUserIdAndIsActive(UUID id, UUID userId, String isActive);

    Optional<FileFolderEntity> findByUserIdAndNameAndParentIdAndIsActive(UUID userId, String name, UUID parentId,
            String isActive);

    @Query("SELECT f FROM FileFolderEntity f WHERE f.userId = :userId AND f.isActive = :isActive ORDER BY f.path")
    List<FileFolderEntity> findAllByUserIdOrderByPath(@Param("userId") UUID userId, @Param("isActive") String isActive);

    @Query("SELECT f FROM FileFolderEntity f WHERE f.userId = :userId AND f.path LIKE :pathPattern AND f.isActive = :isActive")
    List<FileFolderEntity> findByUserIdAndPathStartsWith(@Param("userId") UUID userId,
            @Param("pathPattern") String pathPattern, @Param("isActive") String isActive);

    boolean existsByUserIdAndNameAndParentIdAndIsActive(UUID userId, String name, UUID parentId, String isActive);
}
