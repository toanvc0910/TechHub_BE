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
            "WHERE p.scope = 'GLOBAL' " +
            "AND p.isActive = 'Y' " +
            "AND p.effectiveFrom <= :refTime " +
            "AND (p.effectiveTo IS NULL OR p.effectiveTo > :refTime) " +
            "ORDER BY p.version DESC")
    List<RevenueSplitPolicy> findActiveGlobalPolicies(
            @Param("refTime") OffsetDateTime refTime);

    @Query("SELECT p FROM RevenueSplitPolicy p " +
            "WHERE p.scope = 'INSTRUCTOR' " +
            "AND p.instructorId = :instructorId " +
            "AND p.isActive = 'Y' " +
            "AND p.effectiveFrom <= :refTime " +
            "AND (p.effectiveTo IS NULL OR p.effectiveTo > :refTime) " +
            "ORDER BY p.version DESC")
    List<RevenueSplitPolicy> findActiveInstructorPolicies(
            @Param("instructorId") UUID instructorId,
            @Param("refTime") OffsetDateTime refTime);

    @Query("SELECT p FROM RevenueSplitPolicy p " +
            "WHERE p.scope = 'COURSE' " +
            "AND p.courseId = :courseId " +
            "AND p.isActive = 'Y' " +
            "AND p.effectiveFrom <= :refTime " +
            "AND (p.effectiveTo IS NULL OR p.effectiveTo > :refTime) " +
            "ORDER BY p.version DESC")
    List<RevenueSplitPolicy> findActiveCoursePolicies(
            @Param("courseId") UUID courseId,
            @Param("refTime") OffsetDateTime refTime);

    Optional<RevenueSplitPolicy> findTopByScopeOrderByVersionDesc(RevenueSplitPolicyScope scope);

    List<RevenueSplitPolicy> findByScopeOrderByVersionDesc(RevenueSplitPolicyScope scope);
}