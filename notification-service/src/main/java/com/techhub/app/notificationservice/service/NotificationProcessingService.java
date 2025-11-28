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
        log.info("⚙️ [PROCESSING SERVICE] ===== START PROCESSING =====");
        log.info("⚙️ [PROCESSING SERVICE] Command type: {}, Available processors: {}",
                command.getType(), processors.size());

        for (NotificationProcessor processor : processors) {
            log.debug("⚙️ [PROCESSING SERVICE] Checking processor: {}", processor.getClass().getSimpleName());
            if (processor.supports(command)) {
                log.info("⚙️ [PROCESSING SERVICE] ✅ Found matching processor: {}",
                        processor.getClass().getSimpleName());
                processor.process(command);
                log.info("⚙️ [PROCESSING SERVICE] ✅ Processing completed by {}", processor.getClass().getSimpleName());
                return;
            }
        }
        log.warn("⚙️ [PROCESSING SERVICE] ⚠️ No processor could handle command type: {}", command.getType());
    }
}
