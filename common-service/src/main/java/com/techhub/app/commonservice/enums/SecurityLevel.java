package com.techhub.app.commonservice.enums;

/**
 * Defines the security level for an endpoint:
 * <ul>
 * <li>PUBLIC — no JWT, no permission check</li>
 * <li>AUTHENTICATED — JWT required, no permission check</li>
 * <li>AUTHORIZED — JWT required + RBAC permission check</li>
 * </ul>
 */
public enum SecurityLevel {
    PUBLIC,
    AUTHENTICATED,
    AUTHORIZED
}
