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

    @Query(value = "SELECT * FROM revenue_split_policies p " +
            "WHERE p.scope = 'INSTRUCTOR' " +
            "AND CAST(p.instructor_id AS TEXT) = CAST(:instructorId AS TEXT) " +
            "AND p.is_active = 'Y' " +
            "AND p.effective_from <= :refTime " +
            "AND (p.effective_to IS NULL OR p.effective_to > :refTime) " +
            "ORDER BY p.version DESC", nativeQuery = true)
    List<RevenueSplitPolicy> findActiveInstructorPolicies(
            @Param("instructorId") UUID instructorId,
            @Param("refTime") OffsetDateTime refTime);

    @Query(value = "SELECT * FROM revenue_split_policies p " +
            "WHERE p.scope = 'COURSE' " +
            "AND CAST(p.course_id AS TEXT) = CAST(:courseId AS TEXT) " +
            "AND p.is_active = 'Y' " +
            "AND p.effective_from <= :refTime " +
            "AND (p.effective_to IS NULL OR p.effective_to > :refTime) " +
            "ORDER BY p.version DESC", nativeQuery = true)
    List<RevenueSplitPolicy> findActiveCoursePolicies(
            @Param("courseId") UUID courseId,
            @Param("refTime") OffsetDateTime refTime);

    Optional<RevenueSplitPolicy> findTopByScopeOrderByVersionDesc(RevenueSplitPolicyScope scope);

    List<RevenueSplitPolicy> findByScopeOrderByVersionDesc(RevenueSplitPolicyScope scope);
}