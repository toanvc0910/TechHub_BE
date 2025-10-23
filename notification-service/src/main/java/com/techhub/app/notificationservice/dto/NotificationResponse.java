package com.techhub.app.notificationservice.dto;

import com.techhub.app.commonservice.kafka.event.notification.NotificationDeliveryMethod;
import com.techhub.app.commonservice.kafka.event.notification.NotificationType;
import lombok.Builder;
import lombok.Value;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

@Value
@Builder
public class NotificationResponse {
    UUID id;
    NotificationType type;
    String title;
    String message;
    NotificationDeliveryMethod deliveryMethod;
    boolean read;
    OffsetDateTime createdAt;
    OffsetDateTime sentAt;
    Map<String, Object> metadata;
}
