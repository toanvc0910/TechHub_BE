package com.techhub.app.paymentservice.entity;

import com.techhub.app.commonservice.jpa.PostgreSQLEnumType;
import com.techhub.app.paymentservice.entity.enums.PayoutLedgerEntryType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.GenericGenerator;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.PrePersist;
import javax.persistence.PreUpdate;
import javax.persistence.Table;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

import org.hibernate.annotations.Type;
import org.hibernate.annotations.TypeDef;

@Entity
@Table(name = "payout_ledger_entries")
@TypeDef(name = "pgsql_enum", typeClass = PostgreSQLEnumType.class)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PayoutLedgerEntry {

    @Id
    @GeneratedValue(generator = "UUID")
    @GenericGenerator(name = "UUID", strategy = "org.hibernate.id.UUIDGenerator")
    @Column(name = "id", updatable = false, nullable = false)
    private String id;

    @Column(name = "instructor_id", nullable = false)
    private String instructorId;

    @Enumerated(EnumType.STRING)
    @Type(type = "pgsql_enum", parameters = @org.hibernate.annotations.Parameter(name = "enumClass", value = "com.techhub.app.paymentservice.entity.enums.PayoutLedgerEntryType"))
    @Column(name = "entry_type", nullable = false, length = 20)
    private PayoutLedgerEntryType entryType;

    @Column(name = "amount", nullable = false, precision = 14, scale = 2)
    private BigDecimal amount;

    @Column(name = "reference_id")
    private String referenceId;

    @Column(name = "reference_type", length = 40)
    private String referenceType;

    @Column(name = "note", length = 500)
    private String note;

    @Column(name = "created", nullable = false)
    private OffsetDateTime created;

    @Column(name = "updated", nullable = false)
    private OffsetDateTime updated;

    @Column(name = "is_active", nullable = false, length = 1)
    private String isActive;

    @PrePersist
    protected void onCreate() {
        OffsetDateTime now = OffsetDateTime.now();
        this.created = now;
        this.updated = now;
        if (this.isActive == null) {
            this.isActive = "Y";
        }
    }

    @PreUpdate
    protected void onUpdate() {
        this.updated = OffsetDateTime.now();
    }
}
