package com.techhub.app.userservice.repository;

import com.techhub.app.userservice.entity.Permission;
import com.techhub.app.userservice.enums.PermissionMethod;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface PermissionRepository extends JpaRepository<Permission, UUID> {
    List<Permission> findByUrlAndMethod(String url, PermissionMethod method);
    List<Permission> findByMethodAndIsActive(PermissionMethod method, Boolean isActive);
    List<Permission> findByResourceAndIsActive(String resource, Boolean isActive);
}