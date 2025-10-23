package com.techhub.app.notificationservice.processor;

import com.techhub.app.commonservice.kafka.event.notification.NotificationCommand;

public interface NotificationProcessor {

    boolean supports(NotificationCommand command);

    void process(NotificationCommand command);
}
