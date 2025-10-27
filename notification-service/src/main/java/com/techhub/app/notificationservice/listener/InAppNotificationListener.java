package com.techhub.app.notificationservice.listener;

import com.techhub.app.commonservice.kafka.event.notification.NotificationCommand;
import com.techhub.app.notificationservice.service.handler.InAppNotificationHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class InAppNotificationListener {

    private final InAppNotificationHandler inAppNotificationHandler;

    @KafkaListener(
            topics = "${kafka.topics.notification:notification-commands}",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void onMessage(NotificationCommand command, Acknowledgment acknowledgment) {
        try {
            log.debug("Received IN_APP notification command {}", command.getCommandId());
            inAppNotificationHandler.handle(command);
            acknowledgment.acknowledge();
        } catch (Exception ex) {
            log.error("Failed to process IN_APP notification command {}", command.getCommandId(), ex);
        }
    }
}
