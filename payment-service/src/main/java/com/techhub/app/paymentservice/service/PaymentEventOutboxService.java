package com.techhub.app.paymentservice.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.techhub.app.paymentservice.dto.event.PaymentCompletedEvent;
import com.techhub.app.paymentservice.dto.event.RevenueSplitRecordedEvent;
import com.techhub.app.paymentservice.entity.OutboxEvent;
import com.techhub.app.paymentservice.entity.Transaction;
import com.techhub.app.paymentservice.entity.enums.OutboxStatus;
import com.techhub.app.paymentservice.entity.enums.PaymentMethod;
import com.techhub.app.paymentservice.repository.OutboxEventRepository;
import com.techhub.app.paymentservice.repository.TransactionItemRepository;
import com.techhub.app.paymentservice.repository.projection.RevenueSplitItemProjection;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentEventOutboxService {

    private static final String AGGREGATE_TYPE_TRANSACTION = "TRANSACTION";
    private static final int EVENT_VERSION = 1;

    private final OutboxEventRepository outboxEventRepository;
    private final TransactionItemRepository transactionItemRepository;
    private final RevenueSplitPolicyService revenueSplitPolicyService;
    private final ObjectMapper objectMapper;

    @Transactional
    public void recordPaymentCompleted(Transaction transaction, PaymentMethod method) {
        PaymentCompletedEvent event = PaymentCompletedEvent.builder()
                .transactionId(transaction.getId())
                .userId(transaction.getUserId())
                .amount(transaction.getAmount())
                .paymentMethod(method == null ? "UNKNOWN" : method.name())
                .completedAt(OffsetDateTime.now())
                .build();

        saveIfAbsent(transaction.getId(), "payment.completed", eventKey(transaction.getId(), "payment.completed"),
                event);
    }

    @Transactional
    public void recordRevenueSplit(Transaction transaction) {
        List<RevenueSplitItemProjection> splitItems = transactionItemRepository
                .getRevenueSplitItemsByTransactionId(transaction.getId());
        if (splitItems.isEmpty()) {
            log.warn("No transaction items found for split event. transactionId={}", transaction.getId());
            return;
        }

        OffsetDateTime computedAt = OffsetDateTime.now();
        BigDecimal eventInstructorRate = null;
        BigDecimal eventAdminRate = null;
        Integer eventPolicyVersion = null;
        String eventPolicyScope = null;

        RevenueSplitRecordedEvent.RevenueSplitRecordedEventBuilder builder = RevenueSplitRecordedEvent.builder()
                .transactionId(transaction.getId())
                .computedAt(computedAt);

        for (RevenueSplitItemProjection row : splitItems) {
            UUID courseId = parseUuid(row.getCourseId(), "courseId", transaction.getId());
            UUID instructorId = parseUuid(row.getInstructorId(), "instructorId", transaction.getId());
            if (courseId == null || instructorId == null) {
                continue;
            }

            RevenueSplitPolicyService.ResolvedPolicy resolvedPolicy = revenueSplitPolicyService.resolvePolicy(
                    instructorId,
                    courseId,
                    computedAt);
            BigDecimal normalizedInstructorRate = normalizeRate(resolvedPolicy.getInstructorRate());
            BigDecimal adminRate = BigDecimal.ONE.subtract(normalizedInstructorRate).setScale(4, RoundingMode.HALF_UP);

            BigDecimal grossAmount = safeMoney(row.getGrossAmount());
            BigDecimal instructorAmount = grossAmount.multiply(normalizedInstructorRate).setScale(2,
                    RoundingMode.HALF_UP);
            BigDecimal adminAmount = grossAmount.subtract(instructorAmount).setScale(2, RoundingMode.HALF_UP);

            if (eventInstructorRate == null) {
                eventInstructorRate = normalizedInstructorRate;
                eventAdminRate = adminRate;
                eventPolicyVersion = resolvedPolicy.getVersion();
                eventPolicyScope = resolvedPolicy.getScope() == null ? null : resolvedPolicy.getScope().name();
            }

            builder.item(RevenueSplitRecordedEvent.ItemSplit.builder()
                    .courseId(courseId)
                    .instructorId(instructorId)
                    .policyId(resolvedPolicy.getPolicyId())
                    .policyScope(resolvedPolicy.getScope() == null ? null : resolvedPolicy.getScope().name())
                    .policyVersion(resolvedPolicy.getVersion())
                    .instructorRate(normalizedInstructorRate)
                    .adminRate(adminRate)
                    .grossAmount(grossAmount)
                    .instructorAmount(instructorAmount)
                    .adminAmount(adminAmount)
                    .quantity(row.getQuantity() == null ? 1 : row.getQuantity())
                    .build());
        }

        if (eventInstructorRate != null) {
            builder.instructorRate(eventInstructorRate)
                    .adminRate(eventAdminRate)
                    .policyVersion(eventPolicyVersion)
                    .policyScope(eventPolicyScope);
        }

        saveIfAbsent(transaction.getId(), "revenue.split.recorded", eventKey(transaction.getId(), "revenue.split"),
                builder.build());
    }

    private String eventKey(UUID aggregateId, String eventType) {
        return aggregateId + ":" + eventType + ":v" + EVENT_VERSION;
    }

    private void saveIfAbsent(UUID aggregateId, String eventType, String eventKey, Object payloadObject) {
        if (outboxEventRepository.findByEventKey(eventKey).isPresent()) {
            log.info("Skip outbox event because eventKey already exists. aggregateId={}, eventType={}, eventKey={}",
                    aggregateId, eventType, eventKey);
            return;
        }

        log.info("Creating outbox event. aggregateId={}, eventType={}, eventKey={}, payloadType={}",
                aggregateId, eventType, eventKey,
                payloadObject == null ? "null" : payloadObject.getClass().getSimpleName());
        String payload = serialize(payloadObject);
        OutboxEvent event = OutboxEvent.builder()
                .aggregateType(AGGREGATE_TYPE_TRANSACTION)
                .aggregateId(aggregateId)
                .eventType(eventType)
                .eventKey(eventKey)
                .eventVersion(EVENT_VERSION)
                .payload(payload)
                .status(OutboxStatus.NEW)
                .retryCount(0)
                .build();
        OutboxEvent saved = outboxEventRepository.save(event);
        log.info("Outbox event persisted. id={}, aggregateId={}, eventType={}, eventKey={}, status={}",
                saved.getId(), aggregateId, eventType, eventKey, saved.getStatus());
    }

    private String serialize(Object data) {
        try {
            return objectMapper.copy()
                    .findAndRegisterModules()
                    .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
                    .writeValueAsString(data);
        } catch (JsonProcessingException e) {
            log.error("Outbox payload serialization failed. payloadType={}, message={}",
                    data == null ? "null" : data.getClass().getName(), e.getMessage(), e);
            throw new IllegalStateException("Cannot serialize outbox payload", e);
        }
    }

    private BigDecimal normalizeRate(BigDecimal value) {
        if (value == null) {
            return BigDecimal.valueOf(0.7);
        }
        if (value.compareTo(BigDecimal.ZERO) < 0) {
            return BigDecimal.ZERO;
        }
        if (value.compareTo(BigDecimal.ONE) > 0) {
            return BigDecimal.ONE;
        }
        return value;
    }

    private BigDecimal safeMoney(BigDecimal value) {
        return value == null ? BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP)
                : value.setScale(2, RoundingMode.HALF_UP);
    }

    private UUID parseUuid(String raw, String fieldName, UUID transactionId) {
        if (raw == null || raw.isBlank()) {
            log.warn("Skip revenue split row because {} is empty. transactionId={}", fieldName, transactionId);
            return null;
        }
        try {
            return UUID.fromString(raw);
        } catch (IllegalArgumentException ex) {
            log.warn("Skip revenue split row because {} is invalid UUID. transactionId={}, value={}",
                    fieldName, transactionId, raw);
            return null;
        }
    }
}