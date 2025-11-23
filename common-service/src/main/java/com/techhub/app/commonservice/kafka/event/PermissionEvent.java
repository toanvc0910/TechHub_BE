package com.techhub.app.commonservice.kafka.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

/**
 * Event published when permissions are updated to invalidate cache
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PermissionEvent {

    /**
     * Type of permission update event
     */
    private PermissionEventType eventType;

    /**
     * User ID (for USER_PERMISSION_UPDATED)
     */
    private UUID userId;

    /**
     * Role ID (for ROLE_PERMISSION_UPDATED)
     */
    private UUID roleId;

    /**
     * Role name (for ROLE_PERMISSION_UPDATED)
     */
    private String roleName;

    /**
     * Permission ID (for PERMISSION_UPDATED)
     */
    private UUID permissionId;

    /**
     * Resource path (for PERMISSION_UPDATED)
     */
    private String resource;

    /**
     * Action (for PERMISSION_UPDATED)
     */
    private String action;

    /**
     * Event timestamp
     */
    private Instant timestamp;

    /**
     * Event metadata
     */
    private String metadata;

    public enum PermissionEventType {
        /**
         * User's permissions changed (role assigned/removed, user-specific override)
         */
        USER_PERMISSION_UPDATED,

        /**
         * Role's permissions changed (permission added/removed from role)
         */
        ROLE_PERMISSION_UPDATED,

        /**
         * Permission definition changed (created/updated/deleted)
         */
        PERMISSION_UPDATED
    }
}
