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

    @KafkaListener(topics = "${kafka.topics.notification:notification-commands}", containerFactory = "kafkaListenerContainerFactory")
    public void onMessage(NotificationCommand command, Acknowledgment acknowledgment) {
        log.info("📨 [KAFKA LISTENER] ===== RECEIVED NOTIFICATION COMMAND =====");
        log.info("📨 [KAFKA LISTENER] CommandId: {}", command.getCommandId());
        log.info("📨 [KAFKA LISTENER] Type: {}, Title: {}", command.getType(), command.getTitle());
        log.info("📨 [KAFKA LISTENER] DeliveryMethods: {}", command.getDeliveryMethods());
        log.info("📨 [KAFKA LISTENER] Recipients: {}", command.getRecipients());
        log.info("📨 [KAFKA LISTENER] Metadata: {}", command.getMetadata());

        try {
            log.debug("📨 [KAFKA LISTENER] Starting to process command...");
            notificationProcessingService.process(command);
            acknowledgment.acknowledge();
            log.info("📨 [KAFKA LISTENER] ✅ Command processed and acknowledged successfully");
        } catch (Exception ex) {
            log.error("📨 [KAFKA LISTENER] ❌ Failed to process notification command {}: {}",
                    command.getCommandId(), ex.getMessage(), ex);
        }
        log.info("📨 [KAFKA LISTENER] ===== END =====");
    }
}
