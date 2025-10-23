package com.techhub.app.notificationservice.service;

import com.techhub.app.commonservice.kafka.event.notification.NotificationDeliveryMethod;
import com.techhub.app.commonservice.kafka.event.notification.NotificationType;
import com.techhub.app.notificationservice.dto.NotificationResponse;
import com.techhub.app.notificationservice.entity.Notification;
import com.techhub.app.notificationservice.mapper.NotificationMapper;
import com.techhub.app.notificationservice.repository.NotificationRepository;
import com.techhub.app.notificationservice.service.impl.NotificationServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificationServiceImplTest {

    @Mock
    private NotificationRepository notificationRepository;

    private NotificationMapper notificationMapper;

    @InjectMocks
    private NotificationServiceImpl notificationService;

    @BeforeEach
    void setUp() {
        notificationMapper = new NotificationMapper();
        notificationService = new NotificationServiceImpl(notificationRepository, notificationMapper);
    }

    @Test
    void markAsRead_updatesNotificationAndReturnsResponse() {
        UUID notificationId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        Notification notification = buildNotification(notificationId, userId, false);

        when(notificationRepository.findByIdAndUserIdAndIsActiveTrue(notificationId, userId))
                .thenReturn(Optional.of(notification));
        when(notificationRepository.save(any(Notification.class))).thenReturn(notification);

        NotificationResponse response = notificationService.markAsRead(notificationId, userId);

        assertThat(response.isRead()).isTrue();
        verify(notificationRepository, times(1)).save(notification);
    }

    @Test
    void markAllAsRead_marksUnread() {
        UUID userId = UUID.randomUUID();
        when(notificationRepository.markAllAsRead(Mockito.eq(userId), any(OffsetDateTime.class))).thenReturn(3);

        int updated = notificationService.markAllAsRead(userId);

        assertThat(updated).isEqualTo(3);
        verify(notificationRepository, times(1)).markAllAsRead(Mockito.eq(userId), any(OffsetDateTime.class));
    }

    private Notification buildNotification(UUID notificationId, UUID userId, boolean read) {
        Notification notification = new Notification();
        notification.setId(notificationId);
        notification.setUserId(userId);
        notification.setType(NotificationType.ACCOUNT);
        notification.setTitle("Account update");
        notification.setMessage("Test message");
        notification.setDeliveryMethod(NotificationDeliveryMethod.IN_APP);
        notification.setRead(read);
        notification.setCreated(OffsetDateTime.now().minusHours(1));
        notification.setUpdated(OffsetDateTime.now().minusHours(1));
        notification.setIsActive(Boolean.TRUE);
        return notification;
    }
}
