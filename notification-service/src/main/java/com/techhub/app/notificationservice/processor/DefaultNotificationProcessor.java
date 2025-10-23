package com.techhub.app.notificationservice.processor;

import com.techhub.app.commonservice.kafka.event.notification.NotificationCommand;
import com.techhub.app.notificationservice.service.NotificationDeliveryService;
import com.techhub.app.notificationservice.service.NotificationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Order(Integer.MAX_VALUE)
@Slf4j
public class DefaultNotificationProcessor extends AbstractNotificationProcessor {

    public DefaultNotificationProcessor(NotificationService notificationService,
                                        NotificationDeliveryService notificationDeliveryService) {
        super(notificationService, notificationDeliveryService);
    }

    @Override
    public boolean supports(NotificationCommand command) {
        // default processor supports every command
        return true;
    }

    @Override
    public void process(NotificationCommand command) {
        dispatch(command);
    }
}
