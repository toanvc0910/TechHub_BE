package com.techhub.app.commonservice.kafka.event;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Data
@Builder
public class EmailEvent {
    private UUID eventId;
    private Instant createdAt;
    private String recipient;
    private String subject;
    private String templateCode;
    private Map<String, Object> variables;
    private Map<String, Object> metadata;
}
