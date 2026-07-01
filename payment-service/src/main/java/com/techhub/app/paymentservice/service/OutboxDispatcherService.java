package com.techhub.app.paymentservice.service;

import com.techhub.app.paymentservice.entity.OutboxEvent;
import com.techhub.app.paymentservice.entity.enums.OutboxStatus;
import com.techhub.app.paymentservice.repository.OutboxEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class OutboxDispatcherService {

    private final OutboxEventRepository outboxEventRepository;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final OutboxStatusUpdateService outboxStatusUpdateService;

    @Value("${payment.events.topic:payment.revenue.events}")
    private String topic;

    @Value("${payment.events.max-retry:5}")
    private int maxRetry;

    @Scheduled(fixedDelayString = "${payment.events.dispatch-delay-ms:3000}")
    public void dispatchPendingEvents() {
        try {
            // Load IDs only to avoid detached entity issues if processing takes time
            List<OutboxEvent> newEvents = outboxEventRepository
                    .findTop100ByStatusOrderByCreatedAsc(OutboxStatus.NEW);

            List<OutboxEvent> failedEvents = outboxEventRepository
                    .findTop100ByStatusAndRetryCountLessThanOrderByCreatedAsc(
                            OutboxStatus.FAILED, maxRetry);

            List<OutboxEvent> allPending = new ArrayList<>(newEvents.size() + failedEvents.size());
            allPending.addAll(newEvents);
            allPending.addAll(failedEvents);

            if (allPending.isEmpty()) {
                return;
            }

            log.info("[OutboxDispatcher] Tick - topic={}, count={}", topic, allPending.size());

            for (OutboxEvent event : allPending) {
                processSingleEvent(event);
            }
        } catch (Exception ex) {
            log.error("[OutboxDispatcher] Unexpected error in dispatch loop", ex);
        }
    }

    private void processSingleEvent(OutboxEvent event) {
        try {
            // 1. Send to Kafka
            kafkaTemplate.send(topic, event.getEventKey(), event.getPayload())
                    .get(10, TimeUnit.SECONDS);

            // 2. Mark as success in a separate transaction
            outboxStatusUpdateService.updateStatusSuccess(event.getId(), event.getRetryCount());
            log.info("[OutboxDispatcher] Published event id={}", event.getId());

        } catch (Exception ex) {
            log.warn("[OutboxDispatcher] Failed to publish event id={}, error={}", event.getId(), ex.getMessage());

            int newRetryCount = (event.getRetryCount() == null ? 0 : event.getRetryCount()) + 1;
            String errorMsg = ex.getMessage();
            if (errorMsg != null && errorMsg.length() > 2000) {
                errorMsg = errorMsg.substring(0, 2000);
            }

            // 3. Mark as failed/dead-letter in a separate transaction
            outboxStatusUpdateService.updateStatusFailed(event.getId(), newRetryCount, errorMsg);
        }
    }
}