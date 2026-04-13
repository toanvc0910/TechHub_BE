package com.techhub.app.paymentservice.repository;

import com.techhub.app.paymentservice.entity.RevenueSplitPolicy;
import com.techhub.app.paymentservice.entity.enums.RevenueSplitPolicyScope;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface RevenueSplitPolicyRepository extends JpaRepository<RevenueSplitPolicy, UUID> {

    @Query("SELECT p FROM RevenueSplitPolicy p " +
            "WHERE p.scope = :scope " +
            "AND p.isActive = 'Y' " +
            "AND p.effectiveFrom <= :refTime " +
            "AND (p.effectiveTo IS NULL OR p.effectiveTo > :refTime) " +
            "AND (:instructorId IS NULL OR p.instructorId = :instructorId) " +
            "AND (:courseId IS NULL OR p.courseId = :courseId) " +
            "ORDER BY p.version DESC")
    List<RevenueSplitPolicy> findActivePolicies(
            @Param("scope") RevenueSplitPolicyScope scope,
            @Param("instructorId") UUID instructorId,
            @Param("courseId") UUID courseId,
            @Param("refTime") OffsetDateTime refTime);

    Optional<RevenueSplitPolicy> findTopByScopeOrderByVersionDesc(RevenueSplitPolicyScope scope);

    List<RevenueSplitPolicy> findByScopeOrderByVersionDesc(RevenueSplitPolicyScope scope);
}