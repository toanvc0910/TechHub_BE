package com.techhub.app.fileservice.kafka;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

@Component
@RequiredArgsConstructor
@Slf4j
public class FileEventPublisher {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Value("${kafka.topics.file-uploaded:file-uploaded}")
    private String fileUploadedTopic;

    public boolean publishFileUploaded(FileUploadedEvent event) {
        try {
            kafkaTemplate.send(fileUploadedTopic, event.getFileId().toString(), event)
                    .get(5, TimeUnit.SECONDS);
            log.info("Published FileUploadedEvent for file {}", event.getFileId());
            return true;
        } catch (Exception ex) {
            log.error("Failed to enqueue FileUploadedEvent for file {}", event.getFileId(), ex);
            return false;
        }
    }
}
