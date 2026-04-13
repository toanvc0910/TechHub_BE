package com.techhub.app.paymentservice.entity;

import com.techhub.app.paymentservice.entity.enums.RevenueSplitPolicyScope;
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

@Entity
@Table(name = "revenue_split_policies")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RevenueSplitPolicy {

    @Id
    @GeneratedValue(generator = "UUID")
    @GenericGenerator(name = "UUID", strategy = "org.hibernate.id.UUIDGenerator")
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(name = "scope", nullable = false, length = 20)
    private RevenueSplitPolicyScope scope;

    @Column(name = "instructor_id")
    private UUID instructorId;

    @Column(name = "course_id")
    private UUID courseId;

    @Column(name = "instructor_rate", nullable = false, precision = 6, scale = 4)
    private BigDecimal instructorRate;

    @Column(name = "admin_rate", nullable = false, precision = 6, scale = 4)
    private BigDecimal adminRate;

    @Column(name = "version", nullable = false)
    private Integer version;

    @Column(name = "effective_from", nullable = false)
    private OffsetDateTime effectiveFrom;

    @Column(name = "effective_to")
    private OffsetDateTime effectiveTo;

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