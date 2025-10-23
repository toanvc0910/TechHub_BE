package com.techhub.app.notificationservice.mapper;

import com.techhub.app.notificationservice.dto.NotificationResponse;
import com.techhub.app.notificationservice.entity.Notification;
import org.springframework.stereotype.Component;

@Component
public class NotificationMapper {

    public NotificationResponse toResponse(Notification notification) {
        if (notification == null) {
            return null;
        }
        return NotificationResponse.builder()
                .id(notification.getId())
                .type(notification.getType())
                .title(notification.getTitle())
                .message(notification.getMessage())
                .deliveryMethod(notification.getDeliveryMethod())
                .read(Boolean.TRUE.equals(notification.getRead()))
                .createdAt(notification.getCreated())
                .sentAt(notification.getSentAt())
                .metadata(notification.getMetadata())
                .build();
    }
}
