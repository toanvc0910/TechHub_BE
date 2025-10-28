package com.techhub.app.fileservice.repository;

import com.techhub.app.fileservice.entity.FileEntity;
import com.techhub.app.fileservice.enums.FileTypeEnum;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface FileRepository extends JpaRepository<FileEntity, UUID> {

        // isActive is String type ("Y"/"N"), not Boolean
        List<FileEntity> findByUserIdAndIsActive(UUID userId, String isActive);

        Page<FileEntity> findByUserIdAndIsActive(UUID userId, String isActive, Pageable pageable);

        List<FileEntity> findByUserIdAndFolderIdAndIsActive(UUID userId, UUID folderId, String isActive);

        Page<FileEntity> findByUserIdAndFolderIdAndIsActive(UUID userId, UUID folderId, String isActive,
                        Pageable pageable);

        List<FileEntity> findByUserIdAndFileTypeAndIsActive(UUID userId, FileTypeEnum fileType, String isActive);

        Page<FileEntity> findByUserIdAndFileTypeAndIsActive(UUID userId, FileTypeEnum fileType, String isActive,
                        Pageable pageable);

        Optional<FileEntity> findByIdAndUserIdAndIsActive(UUID id, UUID userId, String isActive);

        Optional<FileEntity> findByCloudinaryPublicIdAndIsActive(String cloudinaryPublicId, String isActive);

        @Query("SELECT f FROM FileEntity f WHERE f.userId = :userId AND " +
                        "(LOWER(f.name) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
                        "LOWER(f.originalName) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
                        "LOWER(f.description) LIKE LOWER(CONCAT('%', :keyword, '%'))) AND " +
                        "f.isActive = :isActive")
        Page<FileEntity> searchByKeyword(@Param("userId") UUID userId, @Param("keyword") String keyword,
                        @Param("isActive") String isActive, Pageable pageable);

        @Query(value = "SELECT * FROM files f WHERE f.user_id = :userId AND :tag = ANY(f.tags) AND f.is_active = 'Y'", nativeQuery = true)
        List<FileEntity> findByUserIdAndTag(@Param("userId") UUID userId, @Param("tag") String tag);

        @Query("SELECT SUM(f.fileSize) FROM FileEntity f WHERE f.userId = :userId AND f.isActive = :isActive")
        Long getTotalFileSizeByUserId(@Param("userId") UUID userId, @Param("isActive") String isActive);

        @Query("SELECT COUNT(f) FROM FileEntity f WHERE f.userId = :userId AND f.isActive = :isActive")
        Long countByUserId(@Param("userId") UUID userId, @Param("isActive") String isActive);

        @Query("SELECT f.fileType, COUNT(f), SUM(f.fileSize) FROM FileEntity f " +
                        "WHERE f.userId = :userId AND f.isActive = :isActive GROUP BY f.fileType")
        List<Object[]> getFileStatisticsByUserId(@Param("userId") UUID userId, @Param("isActive") String isActive);
}
