package com.techhub.app.commonservice.kafka.event.notification;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationRecipient {
    private UUID userId;
    private String email;
    private String username;
    private Map<String, Object> preferences;
}
