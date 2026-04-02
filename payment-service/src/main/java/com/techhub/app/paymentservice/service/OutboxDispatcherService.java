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
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class OutboxDispatcherService {

    private final OutboxEventRepository outboxEventRepository;
    private final KafkaTemplate<String, String> kafkaTemplate;

    @Value("${payment.events.topic:payment.revenue.events}")
    private String topic;

    @Scheduled(fixedDelayString = "${payment.events.dispatch-delay-ms:3000}")
    @Transactional
    public void dispatchPendingEvents() {
        List<OutboxEvent> pending = outboxEventRepository.findTop100ByStatusOrderByCreatedAsc(OutboxStatus.NEW);
        if (pending.isEmpty()) {
            return;
        }

        for (OutboxEvent event : pending) {
            try {
                kafkaTemplate.send(topic, event.getEventKey(), event.getPayload());
                event.setStatus(OutboxStatus.PUBLISHED);
                event.setPublishedAt(OffsetDateTime.now());
                event.setLastError(null);
                outboxEventRepository.save(event);
            } catch (Exception ex) {
                event.setStatus(OutboxStatus.FAILED);
                event.setRetryCount((event.getRetryCount() == null ? 0 : event.getRetryCount()) + 1);
                event.setLastError(ex.getMessage());
                outboxEventRepository.save(event);
                log.error("Failed to publish outbox event id={} key={}", event.getId(), event.getEventKey(), ex);
            }
        }
    }
}