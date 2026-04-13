package com.techhub.app.paymentservice.repository;

import com.techhub.app.paymentservice.entity.PayoutLedgerEntry;
import com.techhub.app.paymentservice.entity.enums.PayoutLedgerEntryType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Repository
public interface PayoutLedgerEntryRepository extends JpaRepository<PayoutLedgerEntry, UUID> {

    List<PayoutLedgerEntry> findByInstructorIdAndIsActiveOrderByCreatedDesc(UUID instructorId, String isActive);

    @Query("SELECT COALESCE(SUM(le.amount), 0) FROM PayoutLedgerEntry le WHERE le.instructorId = :instructorId AND le.entryType IN :types AND le.isActive = 'Y'")
    BigDecimal sumAmountByInstructorAndTypes(@Param("instructorId") UUID instructorId,
            @Param("types") List<PayoutLedgerEntryType> types);

    boolean existsByInstructorIdAndReferenceTypeAndIsActive(UUID instructorId, String referenceType, String isActive);
}
