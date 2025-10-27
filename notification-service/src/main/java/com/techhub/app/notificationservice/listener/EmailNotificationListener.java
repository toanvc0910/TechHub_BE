package com.techhub.app.notificationservice.listener;

import com.techhub.app.commonservice.kafka.event.notification.NotificationCommand;
import com.techhub.app.notificationservice.service.handler.EmailNotificationHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class EmailNotificationListener {

    private final EmailNotificationHandler emailNotificationHandler;

    @KafkaListener(
            topics = "${kafka.topics.email:email-notifications}",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void onMessage(NotificationCommand command, Acknowledgment acknowledgment) {
        try {
            log.debug("Received EMAIL notification command {}", command.getCommandId());
            emailNotificationHandler.handle(command);
            acknowledgment.acknowledge();
        } catch (Exception ex) {
            log.error("Failed to process EMAIL notification command {}", command.getCommandId(), ex);
        }
    }
}
