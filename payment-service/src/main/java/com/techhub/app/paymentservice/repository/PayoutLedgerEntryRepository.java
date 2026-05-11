package com.techhub.app.paymentservice.repository;

import com.techhub.app.paymentservice.entity.PayoutLedgerEntry;
import com.techhub.app.paymentservice.entity.enums.PayoutLedgerEntryType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

@Repository
public interface PayoutLedgerEntryRepository extends JpaRepository<PayoutLedgerEntry, String> {

    @Query(value = "SELECT * FROM payout_ledger_entries le " +
            "WHERE CAST(le.instructor_id AS TEXT) = :instructorId " +
            "AND le.is_active = :isActive ORDER BY le.created DESC", nativeQuery = true)
    List<PayoutLedgerEntry> findByInstructorIdAndIsActiveOrderByCreatedDesc(@Param("instructorId") String instructorId,
            @Param("isActive") String isActive);

    @Query(value = "SELECT COALESCE(SUM(le.amount), 0) FROM payout_ledger_entries le " +
            "WHERE CAST(le.instructor_id AS TEXT) = :instructorId " +
            "AND le.entry_type IN (:types) AND le.is_active = 'Y'", nativeQuery = true)
    BigDecimal sumAmountByInstructorAndTypes(@Param("instructorId") String instructorId,
            @Param("types") Collection<String> types);

    @Query(value = "SELECT CASE WHEN COUNT(1) > 0 THEN TRUE ELSE FALSE END FROM payout_ledger_entries le " +
            "WHERE CAST(le.instructor_id AS TEXT) = :instructorId " +
            "AND le.reference_type = :referenceType AND le.is_active = :isActive", nativeQuery = true)
    boolean existsByInstructorIdAndReferenceTypeAndIsActive(@Param("instructorId") String instructorId,
            @Param("referenceType") String referenceType,
            @Param("isActive") String isActive);

    @Query(value = "SELECT COALESCE(SUM(le.amount), 0) FROM payout_ledger_entries le " +
            "WHERE CAST(le.instructor_id AS TEXT) = :instructorId " +
            "AND le.reference_type = :referenceType AND le.is_active = 'Y'", nativeQuery = true)
    BigDecimal sumAmountByInstructorAndReferenceType(@Param("instructorId") String instructorId,
            @Param("referenceType") String referenceType);
}
