package com.techhub.app.commonservice.dto;

import com.techhub.app.commonservice.enums.SecurityLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * Shared DTO for endpoint security policies.
 * Used by user-service (producer) and proxy-client (consumer).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EndpointSecurityPolicyDTO {
    private UUID id;
    private String urlPattern;
    private String method;
    private SecurityLevel securityLevel;
    private String description;
}
