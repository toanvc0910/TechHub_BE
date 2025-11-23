package com.techhub.app.userservice.service.impl;

import com.techhub.app.commonservice.exception.NotFoundException;
import com.techhub.app.userservice.dto.response.PermissionResponse;
import com.techhub.app.userservice.dto.response.RoleResponse;
import com.techhub.app.userservice.entity.Permission;
import com.techhub.app.userservice.entity.Role;
import com.techhub.app.userservice.entity.RolePermission;
import com.techhub.app.userservice.entity.User;
import com.techhub.app.userservice.entity.UserPermission;
import com.techhub.app.userservice.entity.UserRole;
import com.techhub.app.userservice.enums.PermissionMethod;
import com.techhub.app.userservice.repository.PermissionRepository;
import com.techhub.app.userservice.repository.RolePermissionRepository;
import com.techhub.app.userservice.repository.RoleRepository;
import com.techhub.app.userservice.repository.UserPermissionRepository;
import com.techhub.app.userservice.repository.UserRepository;
import com.techhub.app.userservice.repository.UserRoleRepository;
import com.techhub.app.userservice.service.PermissionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.AntPathMatcher;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class PermissionServiceImpl implements PermissionService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final RolePermissionRepository rolePermissionRepository;
    private final PermissionRepository permissionRepository;
    private final UserPermissionRepository userPermissionRepository;
    private final UserRoleRepository userRoleRepository;

    private final AntPathMatcher pathMatcher = new AntPathMatcher();

    @Override
    @Transactional(readOnly = true)
    public List<PermissionResponse> getEffectivePermissions(UUID userId) {
        EffectivePermissionState state = buildEffectivePermissionState(userId);
        if (state.allowedPermissionIds.isEmpty()) {
            return List.of();
        }

        List<Permission> permissions = permissionRepository.findByIdIn(state.allowedPermissionIds);

        return permissions.stream()
                .filter(permission -> Boolean.TRUE.equals(permission.getIsActive()))
                .map(permission -> toPermissionResponse(
                        permission,
                        state.overrideSources.getOrDefault(permission.getId(), "ROLE"),
                        true))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public boolean hasPermission(UUID userId, String url, PermissionMethod method) {
        EffectivePermissionState state = buildEffectivePermissionState(userId);

        if (state.allowedPermissionIds.isEmpty()) {
            return false;
        }

        List<Permission> permissions = permissionRepository.findByIdIn(state.allowedPermissionIds);

        return permissions.stream()
                .filter(permission -> permission.getMethod() == method && Boolean.TRUE.equals(permission.getIsActive()))
                .anyMatch(permission -> {
                    boolean match = pathMatcher.match(permission.getUrl(), url)
                            || permission.getUrl().equalsIgnoreCase(url);
                    if (match) {
                        log.debug("Permission matched for user {} -> {} {}", userId, method, url);
                    }
                    return match;
                });
    }

    @Override
    @Transactional
    public PermissionResponse upsertUserPermission(UUID userId, UUID permissionId, boolean allowed, boolean active,
            UUID actorId) {
        User user = findActiveUser(userId);
        Permission permission = permissionRepository.findById(permissionId)
                .orElseThrow(() -> new NotFoundException("Permission not found: " + permissionId));

        UserPermission userPermission = userPermissionRepository.findByUserIdAndPermissionId(userId, permissionId)
                .orElseGet(UserPermission::new);

        boolean isNew = userPermission.getUserId() == null;

        userPermission.setUserId(user.getId());
        userPermission.setPermissionId(permission.getId());
        userPermission.setAllowed(allowed);
        userPermission.setIsActive(active);
        userPermission.setAssignedAt(LocalDateTime.now());

        if (actorId != null) {
            if (isNew) {
                userPermission.setCreatedBy(actorId);
            }
            userPermission.setUpdatedBy(actorId);
        }

        userPermissionRepository.save(userPermission);

        return toPermissionResponse(permission, "USER_OVERRIDE", allowed);
    }

    @Override
    @Transactional
    public void deactivateUserPermission(UUID userId, UUID permissionId, UUID actorId) {
        UserPermission userPermission = userPermissionRepository.findByUserIdAndPermissionId(userId, permissionId)
                .orElseThrow(() -> new NotFoundException("User permission not found for user " + userId));

        userPermission.setIsActive(false);
        userPermission.setUpdatedBy(actorId);
        userPermissionRepository.save(userPermission);
    }

    // ===== Admin: Permissions =====
    @Override
    @Transactional(readOnly = true)
    public List<PermissionResponse> listPermissions() {
        return permissionRepository.findAll().stream()
                .filter(p -> Boolean.TRUE.equals(p.getIsActive()))
                .map(p -> toPermissionResponse(p, "ROLE", Boolean.TRUE))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public PermissionResponse getPermissionById(UUID permissionId) {
        Permission permission = permissionRepository.findById(permissionId)
                .orElseThrow(() -> new NotFoundException("Permission not found: " + permissionId));
        return toPermissionResponse(permission, "ROLE", Boolean.TRUE);
    }

    @Override
    @Transactional
    public PermissionResponse createPermission(String name, String description, String url, PermissionMethod method,
            String resource, boolean active, UUID actorId) {
        Permission permission = new Permission();
        permission.setName(name);
        permission.setDescription(description);
        permission.setUrl(url);
        permission.setMethod(method);
        permission.setResource(resource);
        permission.setIsActive(active);
        permission.setCreated(LocalDateTime.now());
        permission.setUpdated(LocalDateTime.now());
        permission.setCreatedBy(actorId);
        permission.setUpdatedBy(actorId);
        Permission saved = permissionRepository.save(permission);
        return toPermissionResponse(saved, "ROLE", Boolean.TRUE);
    }

    @Override
    @Transactional
    public PermissionResponse updatePermission(UUID permissionId, String name, String description, String url,
            PermissionMethod method, String resource, boolean active, UUID actorId) {
        Permission permission = permissionRepository.findById(permissionId)
                .orElseThrow(() -> new NotFoundException("Permission not found: " + permissionId));
        permission.setName(name);
        permission.setDescription(description);
        permission.setUrl(url);
        permission.setMethod(method);
        permission.setResource(resource);
        permission.setIsActive(active);
        permission.setUpdated(LocalDateTime.now());
        permission.setUpdatedBy(actorId);
        Permission saved = permissionRepository.save(permission);
        return toPermissionResponse(saved, "ROLE", Boolean.TRUE);
    }

    @Override
    @Transactional
    public void deletePermission(UUID permissionId, UUID actorId) {

        Permission permission = permissionRepository.findById(permissionId)
                .orElseThrow(() -> {
                    log.error("[SERVICE] Permission not found: {}", permissionId);
                    return new NotFoundException("Permission not found: " + permissionId);
                });

        // Soft delete by setting isActive to false
        permission.setIsActive(false);
        permission.setUpdated(LocalDateTime.now());
        permission.setUpdatedBy(actorId);
        permissionRepository.save(permission);

        // Also deactivate all role-permission and user-permission associations
        List<RolePermission> rolePermissions = rolePermissionRepository.findByPermissionId(permissionId);
        rolePermissions.forEach(rp -> {
            rp.setIsActive(false);
            rp.setUpdated(LocalDateTime.now());
            rp.setUpdatedBy(actorId);
            rolePermissionRepository.save(rp);
        });

        List<UserPermission> userPermissions = userPermissionRepository.findByPermissionId(permissionId);
        userPermissions.forEach(up -> {
            up.setIsActive(false);
            up.setUpdatedBy(actorId);
            userPermissionRepository.save(up);
        });

    }

    // ===== Admin: Roles =====
    @Override
    @Transactional(readOnly = true)
    public List<RoleResponse> listRoles() {
        return roleRepository.findAll().stream()
                .filter(role -> Boolean.TRUE.equals(role.getIsActive()))
                .map(this::toRoleResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public RoleResponse getRoleById(UUID roleId) {
        Role role = roleRepository.findById(roleId)
                .orElseThrow(() -> new NotFoundException("Role not found: " + roleId));
        return toRoleResponse(role);
    }

    @Override
    @Transactional
    public RoleResponse createRole(String name, String description, boolean active, UUID actorId) {
        Role role = new Role();
        role.setName(name);
        role.setDescription(description);
        role.setIsActive(active);
        role.setCreated(LocalDateTime.now());
        role.setUpdated(LocalDateTime.now());
        role.setCreatedBy(actorId);
        role.setUpdatedBy(actorId);
        Role saved = roleRepository.save(role);
        return toRoleResponse(saved);
    }

    @Override
    @Transactional
    public RoleResponse updateRole(UUID roleId, String name, String description, boolean active, UUID actorId) {
        Role role = roleRepository.findById(roleId)
                .orElseThrow(() -> new NotFoundException("Role not found: " + roleId));
        role.setName(name);
        role.setDescription(description);
        role.setIsActive(active);
        role.setUpdated(LocalDateTime.now());
        role.setUpdatedBy(actorId);
        Role saved = roleRepository.save(role);
        return toRoleResponse(saved);
    }

    @Override
    @Transactional
    public void deleteRole(UUID roleId, UUID actorId) {
        log.info("[SERVICE] deleteRole - RoleId: {}, ActorId: {}", roleId, actorId);

        Role role = roleRepository.findById(roleId)
                .orElseThrow(() -> {
                    log.error("[SERVICE] Role not found: {}", roleId);
                    return new NotFoundException("Role not found: " + roleId);
                });

        log.info("[SERVICE] Found role: {} (Active: {})", role.getName(), role.getIsActive());

        // Soft delete by setting isActive to false
        role.setIsActive(false);
        role.setUpdated(LocalDateTime.now());
        role.setUpdatedBy(actorId);
        roleRepository.save(role);
        log.info("[SERVICE] Role marked as inactive");

        // Also deactivate all role-permission and user-role associations
        List<RolePermission> rolePermissions = rolePermissionRepository.findByRoleIdAndIsActive(roleId, true);
        log.info("[SERVICE] Found {} active role-permission associations to deactivate", rolePermissions.size());
        rolePermissions.forEach(rp -> {
            rp.setIsActive(false);
            rp.setUpdated(LocalDateTime.now());
            rp.setUpdatedBy(actorId);
            rolePermissionRepository.save(rp);
        });

        List<UserRole> userRoles = userRoleRepository.findByRoleId(roleId);
        log.info("[SERVICE] Found {} user-role associations to deactivate", userRoles.size());
        userRoles.forEach(ur -> {
            ur.setIsActive(false);
            ur.setUpdated(LocalDateTime.now());
            ur.setUpdatedBy(actorId);
            userRoleRepository.save(ur);
        });

        log.info("[SERVICE] deleteRole completed successfully");
    }

    @Override
    @Transactional
    public void assignPermissionsToRole(UUID roleId, List<UUID> permissionIds, UUID actorId) {
        Role role = roleRepository.findById(roleId)
                .orElseThrow(() -> new NotFoundException("Role not found: " + roleId));
        List<RolePermission> existing = rolePermissionRepository.findByRoleIdAndIsActive(roleId, true);
        Set<UUID> existingIds = existing.stream()
                .map(RolePermission::getPermissionId)
                .collect(Collectors.toSet());

        LocalDateTime now = LocalDateTime.now();
        for (UUID pid : permissionIds) {
            if (!existingIds.contains(pid)) {
                RolePermission rp = new RolePermission();
                rp.setRoleId(role.getId());
                rp.setPermissionId(pid);
                rp.setIsActive(true);
                rp.setCreated(now);
                rp.setUpdated(now);
                rp.setCreatedBy(actorId);
                rp.setUpdatedBy(actorId);
                rolePermissionRepository.save(rp);
            }
        }
    }

    @Override
    @Transactional
    public void removePermissionFromRole(UUID roleId, UUID permissionId, UUID actorId) {
        RolePermission rp = rolePermissionRepository.findByRoleIdAndPermissionIdAndIsActive(roleId, permissionId, true)
                .orElseThrow(() -> new NotFoundException("Role permission not found"));
        rp.setIsActive(false);
        rp.setUpdated(LocalDateTime.now());
        rp.setUpdatedBy(actorId);
        rolePermissionRepository.save(rp);
    }

    @Override
    @Transactional(readOnly = true)
    public List<RoleResponse> getUserRoles(UUID userId) {
        User user = findActiveUser(userId);
        List<UserRole> activeUserRoles = user.getUserRoles() == null ? List.of()
                : user.getUserRoles().stream()
                        .filter(UserRole::getIsActive)
                        .collect(Collectors.toList());

        return activeUserRoles.stream()
                .map(ur -> roleRepository.findById(ur.getRoleId())
                        .map(this::toRoleResponse)
                        .orElse(null))
                .filter(r -> r != null)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void assignRolesToUser(UUID userId, List<UUID> roleIds, boolean active, UUID actorId) {
        User user = findActiveUser(userId);
        LocalDateTime now = LocalDateTime.now();
        for (UUID rid : roleIds) {
            Role role = roleRepository.findById(rid)
                    .orElseThrow(() -> new NotFoundException("Role not found: " + rid));
            UserRole userRole = userRoleRepository.findByUserIdAndRoleId(user.getId(), role.getId())
                    .orElseGet(UserRole::new);
            boolean isNew = userRole.getUserId() == null;
            userRole.setUserId(user.getId());
            userRole.setRoleId(role.getId());
            userRole.setIsActive(active);
            userRole.setUpdated(now);
            if (isNew) {
                userRole.setCreated(now);
                userRole.setAssignedAt(now);
                userRole.setCreatedBy(actorId);
            }
            userRole.setUpdatedBy(actorId);
            userRoleRepository.save(userRole);
        }
    }

    @Override
    @Transactional
    public void removeRoleFromUser(UUID userId, UUID roleId, UUID actorId) {
        UserRole userRole = userRoleRepository.findByUserIdAndRoleId(userId, roleId)
                .orElseThrow(() -> new NotFoundException("User role not found"));
        userRole.setIsActive(false);
        userRole.setUpdated(LocalDateTime.now());
        userRole.setUpdatedBy(actorId);
        userRoleRepository.save(userRole);
    }

    private EffectivePermissionState buildEffectivePermissionState(UUID userId) {
        User user = findActiveUser(userId);

        // Collect all role IDs from dynamic role assignments only
        Set<UUID> roleIds = new HashSet<>();

        List<UserRole> activeUserRoles = user.getUserRoles() == null ? List.of()
                : user.getUserRoles().stream()
                        .filter(UserRole::getIsActive)
                        .collect(Collectors.toList());

        for (UserRole ur : activeUserRoles) {
            if (ur.getRoleId() != null) {
                roleIds.add(ur.getRoleId());
            }
        }

        // Allow-set built from role permissions
        Set<UUID> allowedPermissions = new HashSet<>();
        Map<UUID, String> sources = new HashMap<>();
        for (UUID roleId : roleIds) {
            List<UUID> permissionIds = rolePermissionRepository.findPermissionIdsByRoleId(roleId);
            allowedPermissions.addAll(permissionIds);
            permissionIds.forEach(id -> sources.putIfAbsent(id, "ROLE"));
        }

        // Apply user-level overrides (deny removes, allow adds)
        List<UserPermission> overrides = userPermissionRepository.findActiveByUserId(userId);
        for (UserPermission override : overrides) {
            UUID permissionId = override.getPermissionId();
            sources.put(permissionId, "USER_OVERRIDE");

            if (Boolean.TRUE.equals(override.getAllowed())) {
                allowedPermissions.add(permissionId);
            } else {
                allowedPermissions.remove(permissionId);
            }
        }

        return new EffectivePermissionState(allowedPermissions, sources);
    }

    private PermissionResponse toPermissionResponse(Permission permission, String source, boolean allowed) {
        return PermissionResponse.builder()
                .id(permission.getId())
                .name(permission.getName())
                .description(permission.getDescription())
                .url(permission.getUrl())
                .method(permission.getMethod())
                .resource(permission.getResource())
                .source(source)
                .allowed(allowed)
                .build();
    }

    private RoleResponse toRoleResponse(Role role) {
        List<UUID> permIds = rolePermissionRepository.findPermissionIdsByRoleId(role.getId());
        return RoleResponse.builder()
                .id(role.getId())
                .name(role.getName())
                .description(role.getDescription())
                .isActive(role.getIsActive())
                .permissionIds(permIds)
                .build();
    }

    private User findActiveUser(UUID userId) {
        return userRepository.findByIdAndIsActiveTrue(userId)
                .orElseThrow(() -> new NotFoundException("Active user not found with ID: " + userId));
    }

    private static class EffectivePermissionState {
        private final Set<UUID> allowedPermissionIds;
        private final Map<UUID, String> overrideSources;

        EffectivePermissionState(Set<UUID> allowedPermissionIds, Map<UUID, String> overrideSources) {
            this.allowedPermissionIds = allowedPermissionIds;
            this.overrideSources = overrideSources;
        }
    }
}
