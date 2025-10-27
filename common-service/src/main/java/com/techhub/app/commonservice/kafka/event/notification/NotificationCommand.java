package com.techhub.app.commonservice.kafka.event.notification;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class NotificationCommand {
    private UUID commandId;
    private Instant createdAt;
    private NotificationType type;
    private String title;
    private String message;
    private Set<NotificationDeliveryMethod> deliveryMethods;
    private List<NotificationRecipient> recipients;
    private String templateCode;
    private Map<String, Object> templateVariables;
    private Map<String, Object> metadata;
}
