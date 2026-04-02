package com.techhub.app.analyticsservice.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.GenericGenerator;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.PrePersist;
import javax.persistence.PreUpdate;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "analytics_revenue_daily", uniqueConstraints = {
        @UniqueConstraint(name = "uk_revenue_daily_date_instructor", columnNames = { "metric_date", "instructor_id" })
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RevenueDailyAggregate {

    @Id
    @GeneratedValue(generator = "UUID")
    @GenericGenerator(name = "UUID", strategy = "org.hibernate.id.UUIDGenerator")
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "metric_date", nullable = false)
    private LocalDate metricDate;

    @Column(name = "instructor_id", nullable = false)
    private UUID instructorId;

    @Column(name = "gross_revenue", nullable = false, precision = 14, scale = 2)
    private BigDecimal grossRevenue;

    @Column(name = "instructor_revenue", nullable = false, precision = 14, scale = 2)
    private BigDecimal instructorRevenue;

    @Column(name = "admin_revenue", nullable = false, precision = 14, scale = 2)
    private BigDecimal adminRevenue;

    @Column(name = "order_count", nullable = false)
    private Long orderCount;

    @Column(name = "item_count", nullable = false)
    private Long itemCount;

    @Column(name = "created", nullable = false)
    private OffsetDateTime created;

    @Column(name = "updated", nullable = false)
    private OffsetDateTime updated;

    @PrePersist
    protected void onCreate() {
        OffsetDateTime now = OffsetDateTime.now();
        this.created = now;
        this.updated = now;
        if (grossRevenue == null) {
            grossRevenue = BigDecimal.ZERO;
        }
        if (instructorRevenue == null) {
            instructorRevenue = BigDecimal.ZERO;
        }
        if (adminRevenue == null) {
            adminRevenue = BigDecimal.ZERO;
        }
        if (orderCount == null) {
            orderCount = 0L;
        }
        if (itemCount == null) {
            itemCount = 0L;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        this.updated = OffsetDateTime.now();
    }
}
