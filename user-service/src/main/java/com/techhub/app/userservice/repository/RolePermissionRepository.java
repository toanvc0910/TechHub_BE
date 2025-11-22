package com.techhub.app.userservice.repository;

import com.techhub.app.userservice.entity.RolePermission;
import com.techhub.app.userservice.entity.RolePermissionId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface RolePermissionRepository extends JpaRepository<RolePermission, RolePermissionId> {

    @Query("SELECT rp.permissionId FROM RolePermission rp WHERE rp.roleId = :roleId AND rp.isActive = true")
    List<UUID> findPermissionIdsByRoleId(@Param("roleId") UUID roleId);

    List<RolePermission> findByRoleIdAndIsActiveTrue(UUID roleId);

    List<RolePermission> findByRoleIdAndIsActive(UUID roleId, Boolean isActive);

    List<RolePermission> findByPermissionIdAndIsActive(UUID permissionId, Boolean isActive);

    List<RolePermission> findByPermissionId(UUID permissionId);

    Optional<RolePermission> findByRoleIdAndPermissionIdAndIsActive(UUID roleId, UUID permissionId, Boolean isActive);
}
