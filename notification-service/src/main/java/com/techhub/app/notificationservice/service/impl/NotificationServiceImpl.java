package com.techhub.app.notificationservice.service.impl;

import com.techhub.app.commonservice.exception.NotFoundException;
import com.techhub.app.notificationservice.dto.NotificationResponse;
import com.techhub.app.notificationservice.entity.Notification;
import com.techhub.app.notificationservice.mapper.NotificationMapper;
import com.techhub.app.notificationservice.repository.NotificationRepository;
import com.techhub.app.notificationservice.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationServiceImpl implements NotificationService {

    private final NotificationRepository notificationRepository;
    private final NotificationMapper notificationMapper;

    @Override
    @Transactional(readOnly = true)
    public Page<NotificationResponse> getNotifications(UUID userId, Boolean read, Pageable pageable) {
        if (userId == null) {
            throw new IllegalArgumentException("User id is required to fetch notifications");
        }
        Page<Notification> page = read == null
                ? notificationRepository.findByUserIdAndIsActiveTrue(userId, pageable)
                : notificationRepository.findByUserIdAndIsActiveTrueAndRead(userId, read, pageable);
        return page.map(notificationMapper::toResponse);
    }

    @Override
    @Transactional
    public NotificationResponse markAsRead(UUID notificationId, UUID userId) {
        Notification notification = notificationRepository.findByIdAndUserIdAndIsActiveTrue(notificationId, userId)
                .orElseThrow(() -> new NotFoundException("Notification not found"));

        if (Boolean.FALSE.equals(notification.getRead())) {
            notification.setRead(Boolean.TRUE);
            notification.setUpdated(OffsetDateTime.now());
            notificationRepository.save(notification);
            log.debug("Notification {} marked as read for user {}", notificationId, userId);
        }

        return notificationMapper.toResponse(notification);
    }

    @Override
    @Transactional
    public int markAllAsRead(UUID userId) {
        if (userId == null) {
            return 0;
        }
        int updated = notificationRepository.markAllAsRead(userId, OffsetDateTime.now());
        if (updated > 0) {
            log.debug("Marked {} notifications as read for user {}", updated, userId);
        }
        return updated;
    }

    @Override
    @Transactional
    public Notification createNotification(Notification notification) {
        return notificationRepository.save(notification);
    }
}
