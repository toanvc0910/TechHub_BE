package com.techhub.app.paymentservice.service;

import com.techhub.app.paymentservice.entity.enums.OutboxStatus;
import com.techhub.app.paymentservice.repository.OutboxEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class OutboxStatusUpdateService {

    private final OutboxEventRepository outboxEventRepository;

    @Value("${payment.events.max-retry:5}")
    private int maxRetry;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void updateStatusSuccess(UUID id, Integer currentRetry) {
        outboxEventRepository.markPublished(
                id.toString(),
                OutboxStatus.PUBLISHED.name(),
                currentRetry == null ? 0 : currentRetry,
                OffsetDateTime.now());
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void updateStatusFailed(UUID id, int newRetryCount, String errorMsg) {
        OutboxStatus nextStatus = (newRetryCount >= maxRetry) ? OutboxStatus.DEAD_LETTER : OutboxStatus.FAILED;
        outboxEventRepository.markFailed(id.toString(), nextStatus.name(), newRetryCount, errorMsg);

        if (nextStatus == OutboxStatus.DEAD_LETTER) {
            log.error("[OutboxDispatcher] Event {} moved to DEAD_LETTER after {} retries", id, newRetryCount);
        }
    }
}