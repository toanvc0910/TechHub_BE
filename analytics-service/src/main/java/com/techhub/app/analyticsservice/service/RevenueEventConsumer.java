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
    private final ObjectMapper objectMapper = new ObjectMapper();

    @KafkaListener(topics = "${analytics.events.topic:payment.revenue.events}")
    @Transactional
    public void consume(ConsumerRecord<String, String> record) {
        try {
            String eventKey = record.key() == null ? "" : record.key();
            if (!eventKey.isBlank() && processedEventRepository.existsByEventKey(eventKey)) {
                return;
            }

            String payload = record.value();
            JsonNode root = objectMapper.readTree(payload);
            if (!root.has("items") || !root.has("transactionId")) {
                return;
            }

            String transactionId = root.path("transactionId").asText();
            LocalDate metricDate = root.has("computedAt")
                    ? OffsetDateTime.parse(root.path("computedAt").asText()).withOffsetSameInstant(ZoneOffset.UTC)
                            .toLocalDate()
                    : LocalDate.now(ZoneOffset.UTC);

            Set<UUID> transactionInstructorSet = new HashSet<>();
            for (JsonNode item : root.path("items")) {
                UUID instructorId = UUID.fromString(item.path("instructorId").asText());
                UUID key = UUID.nameUUIDFromBytes((transactionId + "-" + instructorId).getBytes());
                boolean firstItemForTransaction = transactionInstructorSet.add(key);

                projectionService.applyRevenueSplit(
                        instructorId,
                        item.path("grossAmount").decimalValue(),
                        item.path("instructorAmount").decimalValue(),
                        item.path("adminAmount").decimalValue(),
                        item.path("quantity").asInt(1),
                        metricDate,
                        firstItemForTransaction);
            }

            if (!eventKey.isBlank()) {
                processedEventRepository.save(ProcessedEvent.builder().eventKey(eventKey).build());
            }
        } catch (Exception ex) {
            log.error("Failed to consume analytics event payload", ex);
        }
    }
}
