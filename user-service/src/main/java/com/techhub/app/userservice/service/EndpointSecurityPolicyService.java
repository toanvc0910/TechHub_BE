package com.techhub.app.userservice.service;

import com.techhub.app.commonservice.dto.EndpointSecurityPolicyDTO;
import com.techhub.app.commonservice.enums.SecurityLevel;

import java.util.List;
import java.util.UUID;

public interface EndpointSecurityPolicyService {

        List<EndpointSecurityPolicyDTO> listActivePolicies();

        EndpointSecurityPolicyDTO createPolicy(String urlPattern, String method, SecurityLevel securityLevel,
                        String description, UUID actorId);

        EndpointSecurityPolicyDTO updatePolicy(UUID id, String urlPattern, String method, SecurityLevel securityLevel,
                        String description, UUID actorId);

        void deletePolicy(UUID id, UUID actorId);
}
