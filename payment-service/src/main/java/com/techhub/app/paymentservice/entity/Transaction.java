package com.techhub.app.paymentservice.entity;

import com.techhub.app.commonservice.jpa.PostgreSQLEnumType;
import com.techhub.app.paymentservice.entity.enums.TransactionStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.Type;
import org.hibernate.annotations.TypeDef;

import javax.persistence.*;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "transactions")
@TypeDef(name = "pgsql_enum", typeClass = PostgreSQLEnumType.class)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Transaction {

    @Id
    @GeneratedValue(generator = "UUID")
    @GenericGenerator(name = "UUID", strategy = "org.hibernate.id.UUIDGenerator")
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal amount;

    @Column(name = "original_amount", precision = 14, scale = 2)
    private BigDecimal originalAmount;

    @Column(name = "original_currency", length = 3)
    private String originalCurrency;

    @Column(name = "gateway_amount", precision = 14, scale = 2)
    private BigDecimal gatewayAmount;

    @Column(name = "gateway_currency", length = 3)
    private String gatewayCurrency;

    @Column(name = "fx_rate", precision = 18, scale = 8)
    private BigDecimal fxRate;

    @Column(name = "fx_provider", length = 64)
    private String fxProvider;

    @Column(name = "fx_quoted_at")
    private OffsetDateTime fxQuotedAt;

    @Enumerated(EnumType.STRING)
    @Type(type = "pgsql_enum", parameters = @org.hibernate.annotations.Parameter(name = "enumClass", value = "com.techhub.app.paymentservice.entity.enums.TransactionStatus"))
    @Column(name = "status", nullable = false)
    private TransactionStatus status;

    @Column(name = "refund_reason")
    private String refundReason;

    @Column(name = "refund_amount", precision = 10, scale = 2)
    private BigDecimal refundAmount;

    @Column(name = "created", nullable = false)
    private ZonedDateTime created;

    @Column(name = "updated", nullable = false)
    private ZonedDateTime updated;

    @Column(name = "created_by")
    private UUID createdBy;

    @Column(name = "updated_by")
    private UUID updatedBy;

    @Column(name = "is_active", nullable = false, length = 1)
    private String isActive = "Y";

    @OneToMany(mappedBy = "transaction", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<TransactionItem> transactionItems = new ArrayList<>();

    @OneToMany(mappedBy = "transaction", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<Payment> payments = new ArrayList<>();

    @PrePersist
    protected void onCreate() {
        created = ZonedDateTime.now();
        updated = ZonedDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updated = ZonedDateTime.now();
    }
}
