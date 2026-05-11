package com.techhub.app.paymentservice.entity;

import com.techhub.app.commonservice.jpa.PostgreSQLEnumType;
import com.techhub.app.paymentservice.entity.enums.InvoiceStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.Type;
import org.hibernate.annotations.TypeDef;

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

@Entity
@Table(name = "payout_invoices")
@TypeDef(name = "pgsql_enum", typeClass = PostgreSQLEnumType.class)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PayoutInvoice {

    @Id
    @GeneratedValue(generator = "UUID")
    @GenericGenerator(name = "UUID", strategy = "org.hibernate.id.UUIDGenerator")
    @Column(name = "id", updatable = false, nullable = false)
    private String id;

    @Column(name = "invoice_number", nullable = false, unique = true, length = 64)
    private String invoiceNumber;

    @Column(name = "payout_request_id", nullable = false, unique = true)
    private String payoutRequestId;

    @Column(name = "instructor_id", nullable = false)
    private String instructorId;

    @Column(name = "amount", nullable = false, precision = 14, scale = 2)
    private BigDecimal amount;

    @Column(name = "transfer_reference", length = 120)
    private String transferReference;

    @Column(name = "pdf_url", length = 500)
    private String pdfUrl;

    @Column(name = "email_sent", nullable = false)
    private Boolean emailSent;

    @Column(name = "ui_visible", nullable = false)
    private Boolean uiVisible;

    @Enumerated(EnumType.STRING)
    @Type(type = "pgsql_enum", parameters = @org.hibernate.annotations.Parameter(name = "enumClass", value = "com.techhub.app.paymentservice.entity.enums.InvoiceStatus"))
    @Column(name = "status", nullable = false, length = 20)
    private InvoiceStatus status;

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
        if (this.emailSent == null) {
            this.emailSent = Boolean.FALSE;
        }
        if (this.uiVisible == null) {
            this.uiVisible = Boolean.TRUE;
        }
        if (this.status == null) {
            this.status = InvoiceStatus.GENERATED;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        this.updated = OffsetDateTime.now();
    }
}