package com.techhub.app.blogservice.repository;

import com.techhub.app.blogservice.entity.Blog;
import com.techhub.app.blogservice.enums.BlogStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface BlogRepository extends JpaRepository<Blog, UUID> {

    Optional<Blog> findByIdAndIsActiveTrue(UUID id);

    Page<Blog> findByStatusAndIsActiveTrueOrderByCreatedDesc(BlogStatus status, Pageable pageable);

    Page<Blog> findByIsActiveTrueOrderByCreatedDesc(Pageable pageable);

    @Query(value = "SELECT * FROM blogs WHERE is_active = 'Y' AND status = :status AND ( :keyword IS NULL OR LOWER(title) LIKE LOWER(CONCAT('%', :keyword, '%')) OR :keyword = ANY(tags) )",
           countQuery = "SELECT COUNT(*) FROM blogs WHERE is_active = 'Y' AND status = :status AND ( :keyword IS NULL OR LOWER(title) LIKE LOWER(CONCAT('%', :keyword, '%')) OR :keyword = ANY(tags) )",
           nativeQuery = true)
    Page<Blog> searchPublished(@Param("status") String status, @Param("keyword") String keyword, Pageable pageable);

    @Query(value = "SELECT * FROM blogs WHERE is_active = 'Y' AND ( :keyword IS NULL OR LOWER(title) LIKE LOWER(CONCAT('%', :keyword, '%')) OR :keyword = ANY(tags) )",
           countQuery = "SELECT COUNT(*) FROM blogs WHERE is_active = 'Y' AND ( :keyword IS NULL OR LOWER(title) LIKE LOWER(CONCAT('%', :keyword, '%')) OR :keyword = ANY(tags) )",
           nativeQuery = true)
    Page<Blog> searchAll(@Param("keyword") String keyword, Pageable pageable);
}
