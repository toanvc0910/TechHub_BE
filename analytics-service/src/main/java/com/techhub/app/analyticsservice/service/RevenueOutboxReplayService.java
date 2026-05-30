package com.techhub.app.analyticsservice.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class RevenueOutboxReplayService {

    private static final int REPLAY_BATCH_SIZE = 500;

    private final JdbcTemplate jdbcTemplate;
    private final RevenueEventConsumer revenueEventConsumer;

    public void replayPendingRevenueEvents() {
        List<RevenueOutboxEvent> pendingEvents = jdbcTemplate.query(
                "SELECT o.event_key, o.payload " +
                        "FROM outbox_events o " +
                        "LEFT JOIN analytics_processed_events p ON p.event_key = o.event_key " +
                        "WHERE o.event_type = 'revenue.split.recorded' " +
                        "AND p.event_key IS NULL " +
                        "ORDER BY o.created ASC " +
                        "LIMIT ?",
                (resultSet, rowNum) -> new RevenueOutboxEvent(
                        resultSet.getString("event_key"),
                        resultSet.getString("payload")),
                REPLAY_BATCH_SIZE);

        if (pendingEvents.isEmpty()) {
            return;
        }

        log.info("[AnalyticsReplay] Replaying {} pending revenue event(s)", pendingEvents.size());
        for (RevenueOutboxEvent event : pendingEvents) {
            try {
                revenueEventConsumer.processEvent(event.eventKey, event.payload);
            } catch (Exception ex) {
                log.error("[AnalyticsReplay] Failed eventKey={}", event.eventKey, ex);
            }
        }
    }

    private static final class RevenueOutboxEvent {
        private final String eventKey;
        private final String payload;

        private RevenueOutboxEvent(String eventKey, String payload) {
            this.eventKey = eventKey;
            this.payload = payload;
        }
    }
}
