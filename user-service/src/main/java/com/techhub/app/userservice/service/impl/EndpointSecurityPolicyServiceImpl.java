package com.techhub.app.userservice.service.impl;

import com.techhub.app.commonservice.dto.EndpointSecurityPolicyDTO;
import com.techhub.app.commonservice.enums.SecurityLevel;
import com.techhub.app.commonservice.exception.NotFoundException;
import com.techhub.app.commonservice.kafka.publisher.EndpointSecurityEventPublisher;
import com.techhub.app.userservice.entity.EndpointSecurityPolicy;
import com.techhub.app.userservice.repository.EndpointSecurityPolicyRepository;
import com.techhub.app.userservice.service.EndpointSecurityPolicyService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class EndpointSecurityPolicyServiceImpl implements EndpointSecurityPolicyService {

        private final EndpointSecurityPolicyRepository repository;
        private final EndpointSecurityEventPublisher eventPublisher;

        @Override
        @Transactional(readOnly = true)
        public List<EndpointSecurityPolicyDTO> listActivePolicies() {
                return repository.findByIsActiveTrue().stream()
                                .map(this::toDTO)
                                .collect(Collectors.toList());
        }

        @Override
        @Transactional
        public EndpointSecurityPolicyDTO createPolicy(String urlPattern, String method, SecurityLevel securityLevel,
                        String description, UUID actorId) {
                EndpointSecurityPolicy policy = new EndpointSecurityPolicy();
                policy.setUrlPattern(urlPattern);
                policy.setMethod(method != null ? method.toUpperCase() : "*");
                policy.setSecurityLevel(securityLevel);
                policy.setDescription(description);
                policy.setIsActive(true);
                policy.setCreatedBy(actorId);
                policy.setUpdatedBy(actorId);

                EndpointSecurityPolicy saved = repository.save(policy);
                eventPublisher.publishPolicyUpdated();
                log.info("Created endpoint security policy: {} {} -> {}", saved.getMethod(), saved.getUrlPattern(),
                                saved.getSecurityLevel());
                return toDTO(saved);
        }

        @Override
        @Transactional
        public EndpointSecurityPolicyDTO updatePolicy(UUID id, String urlPattern, String method,
                        SecurityLevel securityLevel, String description, UUID actorId) {
                EndpointSecurityPolicy policy = repository.findById(id)
                                .orElseThrow(() -> new NotFoundException("Endpoint security policy not found: " + id));

                policy.setUrlPattern(urlPattern);
                policy.setMethod(method != null ? method.toUpperCase() : "*");
                policy.setSecurityLevel(securityLevel);
                policy.setDescription(description);
                policy.setUpdatedBy(actorId);

                EndpointSecurityPolicy saved = repository.save(policy);
                eventPublisher.publishPolicyUpdated();
                log.info("Updated endpoint security policy: {} {} -> {}", saved.getMethod(), saved.getUrlPattern(),
                                saved.getSecurityLevel());
                return toDTO(saved);
        }

        @Override
        @Transactional
        public void deletePolicy(UUID id, UUID actorId) {
                EndpointSecurityPolicy policy = repository.findById(id)
                                .orElseThrow(() -> new NotFoundException("Endpoint security policy not found: " + id));
                policy.setIsActive(false);
                policy.setUpdatedBy(actorId);
                repository.save(policy);
                eventPublisher.publishPolicyUpdated();
                log.info("Deleted (soft) endpoint security policy: {}", id);
        }

        private EndpointSecurityPolicyDTO toDTO(EndpointSecurityPolicy entity) {
                return EndpointSecurityPolicyDTO.builder()
                                .id(entity.getId())
                                .urlPattern(entity.getUrlPattern())
                                .method(entity.getMethod())
                                .securityLevel(entity.getSecurityLevel())
                                .description(entity.getDescription())
                                .build();
        }
}
