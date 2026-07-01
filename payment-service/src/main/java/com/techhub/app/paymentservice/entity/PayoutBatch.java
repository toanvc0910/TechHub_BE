package com.techhub.app.paymentservice.entity;

import com.techhub.app.commonservice.jpa.PostgreSQLEnumType;
import com.techhub.app.paymentservice.entity.enums.PayoutBatchStatus;
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
@Table(name = "payout_batches")
@TypeDef(name = "pgsql_enum", typeClass = PostgreSQLEnumType.class)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PayoutBatch {

    @Id
    @GeneratedValue(generator = "UUID")
    @GenericGenerator(name = "UUID", strategy = "org.hibernate.id.UUIDGenerator")
    @Column(name = "id", updatable = false, nullable = false)
    private String id;

    @Column(name = "batch_name", nullable = false, length = 120)
    private String batchName;

    @Column(name = "period_key", nullable = false, length = 7)
    private String periodKey;

    @Column(name = "from_date", nullable = false)
    private OffsetDateTime fromDate;

    @Column(name = "to_date", nullable = false)
    private OffsetDateTime toDate;

    @Enumerated(EnumType.STRING)
    @Type(type = "pgsql_enum", parameters = @org.hibernate.annotations.Parameter(name = "enumClass", value = "com.techhub.app.paymentservice.entity.enums.PayoutBatchStatus"))
    @Column(name = "status", nullable = false, length = 20)
    private PayoutBatchStatus status;

    @Column(name = "total_requests", nullable = false)
    private Integer totalRequests;

    @Column(name = "total_amount", nullable = false, precision = 14, scale = 2)
    private BigDecimal totalAmount;

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
        if (this.status == null) {
            this.status = PayoutBatchStatus.DRAFT;
        }
        if (this.totalRequests == null) {
            this.totalRequests = 0;
        }
        if (this.totalAmount == null) {
            this.totalAmount = BigDecimal.ZERO;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        this.updated = OffsetDateTime.now();
    }
}
