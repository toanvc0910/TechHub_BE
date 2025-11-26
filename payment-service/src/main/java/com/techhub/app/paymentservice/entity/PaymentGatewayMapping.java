package com.techhub.app.paymentservice.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.GenericGenerator;

import javax.persistence.*;
import java.time.ZonedDateTime;
import java.util.UUID;

@Entity
@Table(name = "payment_gateway_mappings")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentGatewayMapping {

    @Id
    @GeneratedValue(generator = "UUID")
    @GenericGenerator(name = "UUID", strategy = "org.hibernate.id.UUIDGenerator")
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "gateway_order_id", nullable = false, unique = true)
    private String gatewayOrderId; // PayPal order ID, VNPay txnRef, etc.

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "transaction_id", nullable = false, insertable = false, updatable = false)
    private Transaction transaction;

    @Column(name = "transaction_id", nullable = false)
    private UUID transactionId;

    @Column(name = "gateway_type", nullable = false)
    private String gatewayType; // PAYPAL, VNPAY, MOMO, etc.

    @Column(name = "created", nullable = false)
    private ZonedDateTime created;

    @PrePersist
    protected void onCreate() {
        created = ZonedDateTime.now();
    }
}
