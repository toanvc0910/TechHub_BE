package com.techhub.app.userservice.repository;

import com.techhub.app.userservice.entity.Permission;
import com.techhub.app.userservice.enums.PermissionMethod;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Set;
import java.util.UUID;

@Repository
public interface PermissionRepository extends JpaRepository<Permission, UUID> {
    List<Permission> findByUrlAndMethod(String url, PermissionMethod method);

    List<Permission> findByMethodAndIsActive(PermissionMethod method, Boolean isActive);

    List<Permission> findByResourceAndIsActive(String resource, Boolean isActive);

    List<Permission> findByIdIn(Set<UUID> ids);

    List<Permission> findAllByIsActive(String isActive);

    @Query("SELECT p FROM Permission p " +
            "WHERE p.isActive = true " +
            "AND (:search IS NULL OR :search = '' " +
            "OR LOWER(p.name) LIKE LOWER(CONCAT('%', :search, '%')) " +
            "OR LOWER(COALESCE(p.description, '')) LIKE LOWER(CONCAT('%', :search, '%')) " +
            "OR LOWER(p.url) LIKE LOWER(CONCAT('%', :search, '%')) " +
            "OR LOWER(p.resource) LIKE LOWER(CONCAT('%', :search, '%'))) " +
            "ORDER BY p.resource ASC, p.name ASC")
    Page<Permission> searchActivePermissions(@Param("search") String search, Pageable pageable);
}
