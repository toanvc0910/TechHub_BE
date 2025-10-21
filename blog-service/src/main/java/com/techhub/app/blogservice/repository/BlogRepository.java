package com.techhub.app.blogservice.repository;

import com.techhub.app.blogservice.entity.Blog;
import com.techhub.app.blogservice.enums.BlogStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface BlogRepository extends JpaRepository<Blog, UUID> {

    Optional<Blog> findByIdAndIsActiveTrue(UUID id);

    Page<Blog> findByStatusAndIsActiveTrueOrderByCreatedDesc(BlogStatus status, Pageable pageable);

    Page<Blog> findByIsActiveTrueOrderByCreatedDesc(Pageable pageable);

    @Query(
            value = "SELECT * FROM blogs " +
                    "WHERE is_active = 'Y' " +
                    "  AND status = :status " +
                    "  AND ( " +
                    "        :keyword IS NULL " +
                    "        OR :keyword = '' " +
                    "        OR LOWER(title) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
                    "        OR EXISTS ( " +
                    "            SELECT 1 FROM UNNEST(tags) AS tag(tag_value) " +
                    "            WHERE LOWER(tag_value) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
                    "        ) " +
                    "      ) " +
                    "  AND ( " +
                    "        :tags IS NULL " +
                    "        OR CARDINALITY(:tags) = 0 " +
                    "        OR EXISTS ( " +
                    "            SELECT 1 " +
                    "            FROM UNNEST(tags) AS tag(tag_value) " +
                    "            WHERE LOWER(tag_value) = ANY(COALESCE(:tags, ARRAY[]::text[])) " +
                    "        ) " +
                    "      ) " +
                    "ORDER BY created DESC",
            countQuery = "SELECT COUNT(*) FROM blogs " +
                    "WHERE is_active = 'Y' " +
                    "  AND status = :status " +
                    "  AND ( " +
                    "        :keyword IS NULL " +
                    "        OR :keyword = '' " +
                    "        OR LOWER(title) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
                    "        OR EXISTS ( " +
                    "            SELECT 1 FROM UNNEST(tags) AS tag(tag_value) " +
                    "            WHERE LOWER(tag_value) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
                    "        ) " +
                    "      ) " +
                    "  AND ( " +
                    "        :tags IS NULL " +
                    "        OR CARDINALITY(:tags) = 0 " +
                    "        OR EXISTS ( " +
                    "            SELECT 1 " +
                    "            FROM UNNEST(tags) AS tag(tag_value) " +
                    "            WHERE LOWER(tag_value) = ANY(COALESCE(:tags, ARRAY[]::text[])) " +
                    "        ) " +
                    "      )",
            nativeQuery = true
    )
    Page<Blog> searchPublished(@Param("status") String status,
                               @Param("keyword") String keyword,
                               @Param("tags") List<String> tags,
                               Pageable pageable);

    @Query(
            value = "SELECT * FROM blogs " +
                    "WHERE is_active = 'Y' " +
                    "  AND ( " +
                    "        :keyword IS NULL " +
                    "        OR :keyword = '' " +
                    "        OR LOWER(title) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
                    "        OR EXISTS ( " +
                    "            SELECT 1 FROM UNNEST(tags) AS tag(tag_value) " +
                    "            WHERE LOWER(tag_value) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
                    "        ) " +
                    "      ) " +
                    "  AND ( " +
                    "        :tags IS NULL " +
                    "        OR CARDINALITY(:tags) = 0 " +
                    "        OR EXISTS ( " +
                    "            SELECT 1 " +
                    "            FROM UNNEST(tags) AS tag(tag_value) " +
                    "            WHERE LOWER(tag_value) = ANY(COALESCE(:tags, ARRAY[]::text[])) " +
                    "        ) " +
                    "      ) " +
                    "ORDER BY created DESC",
            countQuery = "SELECT COUNT(*) FROM blogs " +
                    "WHERE is_active = 'Y' " +
                    "  AND ( " +
                    "        :keyword IS NULL " +
                    "        OR :keyword = '' " +
                    "        OR LOWER(title) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
                    "        OR EXISTS ( " +
                    "            SELECT 1 FROM UNNEST(tags) AS tag(tag_value) " +
                    "            WHERE LOWER(tag_value) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
                    "        ) " +
                    "      ) " +
                    "  AND ( " +
                    "        :tags IS NULL " +
                    "        OR CARDINALITY(:tags) = 0 " +
                    "        OR EXISTS ( " +
                    "            SELECT 1 " +
                    "            FROM UNNEST(tags) AS tag(tag_value) " +
                    "            WHERE LOWER(tag_value) = ANY(COALESCE(:tags, ARRAY[]::text[])) " +
                    "        ) " +
                    "      )",
            nativeQuery = true
    )
    Page<Blog> searchAll(@Param("keyword") String keyword,
                         @Param("tags") List<String> tags,
                         Pageable pageable);

    @Query(
            value = "SELECT DISTINCT tag_value " +
                    "FROM blogs CROSS JOIN LATERAL UNNEST(tags) AS tag(tag_value) " +
                    "WHERE is_active = 'Y' " +
                    "  AND tag_value IS NOT NULL " +
                    "  AND TRIM(tag_value) <> '' " +
                    "ORDER BY tag_value",
            nativeQuery = true
    )
    List<String> findDistinctTags();
}
