package com.techhub.app.notificationservice.service;

import com.techhub.app.commonservice.kafka.event.notification.NotificationCommand;
import com.techhub.app.notificationservice.processor.NotificationProcessor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationProcessingService {

    private final List<NotificationProcessor> processors;

    public void process(NotificationCommand command) {
        for (NotificationProcessor processor : processors) {
            if (processor.supports(command)) {
                log.debug("Processing notification command {} with processor {}", command.getCommandId(), processor.getClass().getSimpleName());
                processor.process(command);
                return;
            }
        }
        log.warn("No notification processor could handle command {}", command.getCommandId());
    }
}
