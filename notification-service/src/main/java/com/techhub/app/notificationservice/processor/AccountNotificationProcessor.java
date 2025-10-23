package com.techhub.app.notificationservice.processor;

import com.techhub.app.commonservice.kafka.event.notification.NotificationCommand;
import com.techhub.app.commonservice.kafka.event.notification.NotificationRecipient;
import com.techhub.app.commonservice.kafka.event.notification.NotificationType;
import com.techhub.app.notificationservice.entity.Notification;
import com.techhub.app.notificationservice.service.NotificationDeliveryService;
import com.techhub.app.notificationservice.service.NotificationService;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class AccountNotificationProcessor extends AbstractNotificationProcessor {

    private static final String DEFAULT_TITLE = "Account Notification";
    private static final String DEFAULT_MESSAGE = "Your account has a new update.";

    public AccountNotificationProcessor(NotificationService notificationService,
                                        NotificationDeliveryService notificationDeliveryService) {
        super(notificationService, notificationDeliveryService);
    }

    @Override
    public boolean supports(NotificationCommand command) {
        return command != null && command.getType() == NotificationType.ACCOUNT;
    }

    @Override
    public void process(NotificationCommand command) {
        dispatch(command);
    }

    @Override
    protected Notification buildNotification(NotificationCommand command, NotificationRecipient recipient) {
        Notification notification = super.buildNotification(command, recipient);
        if (!StringUtils.hasText(notification.getTitle())) {
            notification.setTitle(DEFAULT_TITLE);
        }
        return notification;
    }

    @Override
    protected String buildFallbackMessage(NotificationCommand command, NotificationRecipient recipient) {
        return DEFAULT_MESSAGE;
    }
}
