package com.techhub.app.notificationservice.listener;

import com.techhub.app.commonservice.kafka.event.notification.NotificationCommand;
import com.techhub.app.notificationservice.service.NotificationProcessingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class NotificationCommandListener {

    private final NotificationProcessingService notificationProcessingService;

    @KafkaListener(
            topics = "${kafka.topics.notification:notification-commands}",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void onMessage(NotificationCommand command, Acknowledgment acknowledgment) {
        try {
            log.debug("Received notification command {}", command.getCommandId());
            notificationProcessingService.process(command);
            acknowledgment.acknowledge();
        } catch (Exception ex) {
            log.error("Failed to process notification command {}", command.getCommandId(), ex);
        }
    }
}
