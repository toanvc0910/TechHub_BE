package com.techhub.app.paymentservice.entity;

import com.techhub.app.paymentservice.config.PostgreSQLEnumType;
import com.techhub.app.paymentservice.entity.enums.PaymentMethod;
import com.techhub.app.paymentservice.entity.enums.PaymentStatus;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.Type;
import org.hibernate.annotations.TypeDef;
import org.hibernate.annotations.TypeDefs;

import javax.persistence.*;
import java.io.IOException;
import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "payments")
@TypeDefs({
    @TypeDef(name = "pgsql_enum", typeClass = PostgreSQLEnumType.class)
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Slf4j
public class Payment {

    @Id
    @GeneratedValue(generator = "UUID")
    @GenericGenerator(name = "UUID", strategy = "org.hibernate.id.UUIDGenerator")
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "transaction_id", nullable = false)
    private Transaction transaction;

    @Enumerated(EnumType.STRING)
    @Type(type = "pgsql_enum")
    @Column(name = "method", nullable = false)
    private PaymentMethod method;

    @Enumerated(EnumType.STRING)
    @Type(type = "pgsql_enum")
    @Column(name = "status", nullable = false)
    private PaymentStatus status;

    @Column(name = "gateway_response", columnDefinition = "jsonb")
    private String gatewayResponseJson;

    @Transient
    private Map<String, Object> gatewayResponse;

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

    @PrePersist
    protected void onCreate() {
        created = ZonedDateTime.now();
        updated = ZonedDateTime.now();
        convertMapToJson();
    }

    @PreUpdate
    protected void onUpdate() {
        updated = ZonedDateTime.now();
        convertMapToJson();
    }

    @PostLoad
    protected void onLoad() {
        convertJsonToMap();
    }

    private void convertMapToJson() {
        if (gatewayResponse != null) {
            try {
                ObjectMapper mapper = new ObjectMapper();
                this.gatewayResponseJson = mapper.writeValueAsString(gatewayResponse);
            } catch (JsonProcessingException e) {
                log.error("Error converting gateway response to JSON", e);
                this.gatewayResponseJson = "{}";
            }
        }
    }

    private void convertJsonToMap() {
        if (gatewayResponseJson != null && !gatewayResponseJson.isEmpty()) {
            try {
                ObjectMapper mapper = new ObjectMapper();
                this.gatewayResponse = mapper.readValue(gatewayResponseJson, Map.class);
            } catch (IOException e) {
                log.error("Error converting JSON to gateway response map", e);
                this.gatewayResponse = new HashMap<>();
            }
        } else {
            this.gatewayResponse = new HashMap<>();
        }
    }

    public Map<String, Object> getGatewayResponse() {
        if (gatewayResponse == null) {
            convertJsonToMap();
        }
        return gatewayResponse;
    }

    public void setGatewayResponse(Map<String, Object> gatewayResponse) {
        this.gatewayResponse = gatewayResponse;
        convertMapToJson();
    }
}
