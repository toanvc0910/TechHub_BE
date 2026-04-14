package com.techhub.app.analyticsservice.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.techhub.app.analyticsservice.entity.ProcessedEvent;
import com.techhub.app.analyticsservice.repository.ProcessedEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class RevenueEventConsumer {

    private final RevenueProjectionService projectionService;
    private final ProcessedEventRepository processedEventRepository;
    private final ObjectMapper objectMapper;

    @KafkaListener(topics = "${analytics.events.topic:payment.revenue.events}", autoStartup = "${kafka.consumer.auto-startup:true}")
    @Transactional
    public void consume(ConsumerRecord<String, String> record) {
        try {
            String eventKey = record.key() == null ? "" : record.key();
            log.info("[AnalyticsConsumer] Received record topic={} partition={} offset={} key={}",
                    record.topic(), record.partition(), record.offset(), eventKey);
            if (!eventKey.isBlank() && processedEventRepository.existsByEventKey(eventKey)) {
                log.info("[AnalyticsConsumer] Skip duplicated eventKey={}", eventKey);
                return;
            }

            String payload = record.value();
            JsonNode root = objectMapper.readTree(payload);
            if (!root.has("items") || !root.has("transactionId")) {
                log.warn("[AnalyticsConsumer] Ignore payload missing required fields. eventKey={} payload={}",
                        eventKey, payload);
                return;
            }

            String transactionId = root.path("transactionId").asText();
            LocalDate metricDate = root.has("computedAt")
                    ? OffsetDateTime.parse(root.path("computedAt").asText()).withOffsetSameInstant(ZoneOffset.UTC)
                            .toLocalDate()
                    : LocalDate.now(ZoneOffset.UTC);

            int itemCount = root.path("items").isArray() ? root.path("items").size() : 0;
            log.info("[AnalyticsConsumer] Processing transactionId={} metricDate={} items={}",
                    transactionId, metricDate, itemCount);

            Set<UUID> transactionInstructorSet = new HashSet<>();
            for (JsonNode item : root.path("items")) {
                UUID instructorId = parseUuid(item.path("instructorId").asText());
                if (instructorId == null) {
                    log.warn("[AnalyticsConsumer] Skip item because instructorId is invalid. transactionId={} item={}",
                            transactionId, item);
                    continue;
                }
                UUID key = UUID.nameUUIDFromBytes((transactionId + "-" + instructorId).getBytes());
                boolean firstItemForTransaction = transactionInstructorSet.add(key);
                String policyScope = item.path("policyScope").asText(null);
                Integer policyVersion = item.has("policyVersion") && !item.path("policyVersion").isNull()
                        ? item.path("policyVersion").asInt()
                        : null;

                log.info(
                        "[AnalyticsConsumer] Apply split instructorId={} gross={} instructor={} admin={} qty={} firstItem={}",
                        instructorId,
                        item.path("grossAmount").decimalValue(),
                        item.path("instructorAmount").decimalValue(),
                        item.path("adminAmount").decimalValue(),
                        item.path("quantity").asInt(1),
                        firstItemForTransaction);

                projectionService.applyRevenueSplit(
                        instructorId,
                        item.path("grossAmount").decimalValue(),
                        item.path("instructorAmount").decimalValue(),
                        item.path("adminAmount").decimalValue(),
                        policyScope,
                        policyVersion,
                        item.path("quantity").asInt(1),
                        metricDate,
                        firstItemForTransaction);
            }

            if (!eventKey.isBlank()) {
                processedEventRepository.save(ProcessedEvent.builder().eventKey(eventKey).build());
                log.info("[AnalyticsConsumer] Marked processed eventKey={}", eventKey);
            }
        } catch (Exception ex) {
            log.error("Failed to consume analytics event payload", ex);
        }
    }

    private UUID parseUuid(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            return UUID.fromString(raw);
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }
}
