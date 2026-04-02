package com.techhub.app.paymentservice.service;

import com.fasterxml.jackson.core.JsonProcessingException;
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
import org.springframework.beans.factory.annotation.Value;
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
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${payment.revenue.instructor-rate:0.7}")
    private BigDecimal instructorRate;

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

        BigDecimal normalizedInstructorRate = normalizeRate(instructorRate);
        BigDecimal adminRate = BigDecimal.ONE.subtract(normalizedInstructorRate);

        RevenueSplitRecordedEvent.RevenueSplitRecordedEventBuilder builder = RevenueSplitRecordedEvent.builder()
                .transactionId(transaction.getId())
                .instructorRate(normalizedInstructorRate)
                .adminRate(adminRate)
                .computedAt(OffsetDateTime.now());

        for (RevenueSplitItemProjection row : splitItems) {
            BigDecimal grossAmount = safeMoney(row.getGrossAmount());
            BigDecimal instructorAmount = grossAmount.multiply(normalizedInstructorRate).setScale(2,
                    RoundingMode.HALF_UP);
            BigDecimal adminAmount = grossAmount.subtract(instructorAmount).setScale(2, RoundingMode.HALF_UP);

            builder.item(RevenueSplitRecordedEvent.ItemSplit.builder()
                    .courseId(row.getCourseId())
                    .instructorId(row.getInstructorId())
                    .grossAmount(grossAmount)
                    .instructorAmount(instructorAmount)
                    .adminAmount(adminAmount)
                    .quantity(row.getQuantity() == null ? 1 : row.getQuantity())
                    .build());
        }

        saveIfAbsent(transaction.getId(), "revenue.split.recorded", eventKey(transaction.getId(), "revenue.split"),
                builder.build());
    }

    private String eventKey(UUID aggregateId, String eventType) {
        return aggregateId + ":" + eventType + ":v" + EVENT_VERSION;
    }

    private void saveIfAbsent(UUID aggregateId, String eventType, String eventKey, Object payloadObject) {
        if (outboxEventRepository.findByEventKey(eventKey).isPresent()) {
            return;
        }

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
        outboxEventRepository.save(event);
    }

    private String serialize(Object data) {
        try {
            return objectMapper.writeValueAsString(data);
        } catch (JsonProcessingException e) {
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
}