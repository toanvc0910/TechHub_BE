package com.techhub.app.fileservice.kafka;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class FileEventPublisher {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Value("${kafka.topics.file-uploaded:file-uploaded}")
    private String fileUploadedTopic;

    public void publishFileUploaded(FileUploadedEvent event) {
        kafkaTemplate.send(fileUploadedTopic, event.getFileId().toString(), event);
        log.info("Published FileUploadedEvent for file {}", event.getFileId());
    }
}