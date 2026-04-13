package com.techhub.app.paymentservice.service;

import com.techhub.app.paymentservice.dto.request.UpsertRevenueSplitPolicyRequest;
import com.techhub.app.paymentservice.dto.response.RevenueSplitPolicyResponse;
import com.techhub.app.paymentservice.entity.RevenueSplitPolicy;
import com.techhub.app.paymentservice.entity.enums.RevenueSplitPolicyScope;
import com.techhub.app.paymentservice.repository.RevenueSplitPolicyRepository;
import lombok.Builder;
import lombok.RequiredArgsConstructor;
import lombok.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RevenueSplitPolicyService {

    private final RevenueSplitPolicyRepository revenueSplitPolicyRepository;

    @org.springframework.beans.factory.annotation.Value("${payment.revenue.instructor-rate:0.7}")
    private BigDecimal defaultInstructorRate;

    @Transactional
    public RevenueSplitPolicyResponse createPolicy(UpsertRevenueSplitPolicyRequest request) {
        RevenueSplitPolicyScope scope = parseScope(request.getScope());
        validateScopeRequest(scope, request.getInstructorId(), request.getCourseId());

        OffsetDateTime effectiveFrom = request.getEffectiveFrom() == null
                ? OffsetDateTime.now()
                : request.getEffectiveFrom();
        OffsetDateTime effectiveTo = request.getEffectiveTo();
        if (effectiveTo != null && !effectiveTo.isAfter(effectiveFrom)) {
            throw new IllegalArgumentException("effectiveTo must be later than effectiveFrom");
        }

        BigDecimal instructorRate = normalizeRate(request.getInstructorRate());
        BigDecimal adminRate = BigDecimal.ONE.subtract(instructorRate).setScale(4, RoundingMode.HALF_UP);
        int nextVersion = revenueSplitPolicyRepository.findTopByScopeOrderByVersionDesc(scope)
                .map(RevenueSplitPolicy::getVersion)
                .orElse(0) + 1;

        RevenueSplitPolicy saved = revenueSplitPolicyRepository.save(RevenueSplitPolicy.builder()
                .scope(scope)
                .instructorId(request.getInstructorId())
                .courseId(request.getCourseId())
                .instructorRate(instructorRate)
                .adminRate(adminRate)
                .version(nextVersion)
                .effectiveFrom(effectiveFrom)
                .effectiveTo(effectiveTo)
                .isActive("Y")
                .build());

        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<RevenueSplitPolicyResponse> listByScope(String scopeRaw) {
        RevenueSplitPolicyScope scope = parseScope(scopeRaw);
        return revenueSplitPolicyRepository.findByScopeOrderByVersionDesc(scope)
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public ResolvedPolicy resolvePolicy(UUID instructorId, UUID courseId, OffsetDateTime refTime) {
        OffsetDateTime at = refTime == null ? OffsetDateTime.now() : refTime;
        ensureDefaultGlobalPolicy();

        if (courseId != null) {
            Optional<RevenueSplitPolicy> coursePolicy = findActive(RevenueSplitPolicyScope.COURSE, instructorId,
                    courseId, at);
            if (coursePolicy.isPresent()) {
                return toResolved(coursePolicy.get());
            }
        }

        if (instructorId != null) {
            Optional<RevenueSplitPolicy> instructorPolicy = findActive(RevenueSplitPolicyScope.INSTRUCTOR,
                    instructorId, null, at);
            if (instructorPolicy.isPresent()) {
                return toResolved(instructorPolicy.get());
            }
        }

        Optional<RevenueSplitPolicy> globalPolicy = findActive(RevenueSplitPolicyScope.GLOBAL, null, null, at);
        if (globalPolicy.isPresent()) {
            return toResolved(globalPolicy.get());
        }

        BigDecimal fallbackInstructorRate = normalizeRate(defaultInstructorRate);
        BigDecimal fallbackAdminRate = BigDecimal.ONE.subtract(fallbackInstructorRate).setScale(4,
                RoundingMode.HALF_UP);
        return ResolvedPolicy.builder()
                .policyId(null)
                .scope(RevenueSplitPolicyScope.GLOBAL)
                .version(0)
                .instructorRate(fallbackInstructorRate)
                .adminRate(fallbackAdminRate)
                .build();
    }

    private Optional<RevenueSplitPolicy> findActive(RevenueSplitPolicyScope scope, UUID instructorId, UUID courseId,
            OffsetDateTime at) {
        List<RevenueSplitPolicy> policies = revenueSplitPolicyRepository.findActivePolicies(scope, instructorId,
                courseId, at);
        if (policies.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(policies.get(0));
    }

    @Transactional
    public void ensureDefaultGlobalPolicy() {
        if (revenueSplitPolicyRepository.findTopByScopeOrderByVersionDesc(RevenueSplitPolicyScope.GLOBAL).isPresent()) {
            return;
        }

        BigDecimal instructorRate = normalizeRate(defaultInstructorRate);
        BigDecimal adminRate = BigDecimal.ONE.subtract(instructorRate).setScale(4, RoundingMode.HALF_UP);

        revenueSplitPolicyRepository.save(RevenueSplitPolicy.builder()
                .scope(RevenueSplitPolicyScope.GLOBAL)
                .instructorRate(instructorRate)
                .adminRate(adminRate)
                .version(1)
                .effectiveFrom(OffsetDateTime.now().minusYears(10))
                .effectiveTo(null)
                .isActive("Y")
                .build());
    }

    private RevenueSplitPolicyScope parseScope(String raw) {
        String value = (raw == null || raw.isBlank()) ? "GLOBAL" : raw.trim().toUpperCase();
        try {
            return RevenueSplitPolicyScope.valueOf(value);
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("Invalid scope. Supported: GLOBAL, INSTRUCTOR, COURSE");
        }
    }

    private void validateScopeRequest(RevenueSplitPolicyScope scope, UUID instructorId, UUID courseId) {
        if (scope == RevenueSplitPolicyScope.GLOBAL && (instructorId != null || courseId != null)) {
            throw new IllegalArgumentException("GLOBAL policy must not include instructorId or courseId");
        }
        if (scope == RevenueSplitPolicyScope.INSTRUCTOR && instructorId == null) {
            throw new IllegalArgumentException("INSTRUCTOR policy requires instructorId");
        }
        if (scope == RevenueSplitPolicyScope.COURSE && courseId == null) {
            throw new IllegalArgumentException("COURSE policy requires courseId");
        }
    }

    private BigDecimal normalizeRate(BigDecimal value) {
        BigDecimal rate = value == null ? defaultInstructorRate : value;
        if (rate == null) {
            rate = BigDecimal.valueOf(0.7);
        }
        if (rate.compareTo(BigDecimal.ZERO) < 0 || rate.compareTo(BigDecimal.ONE) > 0) {
            throw new IllegalArgumentException("instructorRate must be between 0 and 1");
        }
        return rate.setScale(4, RoundingMode.HALF_UP);
    }

    private RevenueSplitPolicyResponse toResponse(RevenueSplitPolicy policy) {
        return RevenueSplitPolicyResponse.builder()
                .id(policy.getId())
                .scope(policy.getScope().name())
                .instructorId(policy.getInstructorId())
                .courseId(policy.getCourseId())
                .instructorRate(policy.getInstructorRate())
                .adminRate(policy.getAdminRate())
                .version(policy.getVersion())
                .effectiveFrom(policy.getEffectiveFrom())
                .effectiveTo(policy.getEffectiveTo())
                .isActive(policy.getIsActive())
                .build();
    }

    private ResolvedPolicy toResolved(RevenueSplitPolicy policy) {
        return ResolvedPolicy.builder()
                .policyId(policy.getId())
                .scope(policy.getScope())
                .version(policy.getVersion())
                .instructorRate(policy.getInstructorRate())
                .adminRate(policy.getAdminRate())
                .build();
    }

    @Value
    @Builder
    public static class ResolvedPolicy {
        UUID policyId;
        RevenueSplitPolicyScope scope;
        Integer version;
        BigDecimal instructorRate;
        BigDecimal adminRate;
    }
}