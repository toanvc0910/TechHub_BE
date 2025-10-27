package com.techhub.app.notificationservice.service.handler;

import com.techhub.app.commonservice.kafka.event.notification.NotificationCommand;
import com.techhub.app.commonservice.kafka.event.notification.NotificationDeliveryMethod;
import com.techhub.app.commonservice.kafka.event.notification.NotificationRecipient;
import com.techhub.app.notificationservice.service.NotificationDeliveryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailNotificationHandler {

    private final NotificationDeliveryService notificationDeliveryService;

    public void handle(NotificationCommand command) {
        if (command == null) {
            log.warn("Skipping null notification command for email channel");
            return;
        }
        if (!containsEmailDelivery(command)) {
            log.debug("Command {} does not request EMAIL delivery. Ignored by email handler", command.getCommandId());
            return;
        }
        if (CollectionUtils.isEmpty(command.getRecipients())) {
            log.warn("Notification command {} skipped for email channel due to missing recipients", command.getCommandId());
            return;
        }

        command.getRecipients().forEach(recipient -> sendEmail(command, recipient));
    }

    private void sendEmail(NotificationCommand command, NotificationRecipient recipient) {
        try {
            notificationDeliveryService.deliverEmail(command, recipient);
        } catch (Exception ex) {
            log.error("Failed to deliver email for command {} to recipient {}", command.getCommandId(), recipient, ex);
        }
    }

    private boolean containsEmailDelivery(NotificationCommand command) {
        return !CollectionUtils.isEmpty(command.getDeliveryMethods())
                && command.getDeliveryMethods().contains(NotificationDeliveryMethod.EMAIL);
    }
}
