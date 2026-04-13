package com.techhub.app.paymentservice.entity;

import com.techhub.app.paymentservice.entity.enums.PayoutRequestStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.GenericGenerator;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.PrePersist;
import javax.persistence.PreUpdate;
import javax.persistence.Table;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "payout_requests")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PayoutRequest {

    @Id
    @GeneratedValue(generator = "UUID")
    @GenericGenerator(name = "UUID", strategy = "org.hibernate.id.UUIDGenerator")
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "instructor_id", nullable = false)
    private UUID instructorId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "batch_id")
    private PayoutBatch batch;

    @Column(name = "batch_id", insertable = false, updatable = false)
    private String batchIdRaw;

    @Column(name = "amount", nullable = false, precision = 14, scale = 2)
    private BigDecimal amount;

    @Column(name = "note", length = 500)
    private String note;

    @Column(name = "payment_reference", length = 120)
    private String paymentReference;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private PayoutRequestStatus status;

    @Column(name = "review_note", length = 500)
    private String reviewNote;

    @Column(name = "approved_by")
    private UUID approvedBy;

    @Column(name = "approved_at")
    private OffsetDateTime approvedAt;

    @Column(name = "marked_paid_by")
    private UUID markedPaidBy;

    @Column(name = "marked_paid_at")
    private OffsetDateTime markedPaidAt;

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
            this.status = PayoutRequestStatus.REQUESTED;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        this.updated = OffsetDateTime.now();
    }
}
