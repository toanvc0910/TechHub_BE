package com.techhub.app.notificationservice.service;

import com.techhub.app.notificationservice.dto.NotificationResponse;
import com.techhub.app.notificationservice.entity.Notification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface NotificationService {

    Page<NotificationResponse> getNotifications(UUID userId, Boolean read, Pageable pageable);

    NotificationResponse markAsRead(UUID notificationId, UUID userId);

    int markAllAsRead(UUID userId);

    Notification createNotification(Notification notification);
}
