package com.techhub.app.fileservice.kafka;

import com.techhub.app.fileservice.service.MediaProcessingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class FileProcessingListener {

    private final MediaProcessingService mediaProcessingService;

    @KafkaListener(topics = "${kafka.topics.file-uploaded:file-uploaded}", containerFactory = "fileUploadedEventKafkaListenerContainerFactory")
    public void onFileUploaded(FileUploadedEvent event, Acknowledgment acknowledgment) {
        try {
            mediaProcessingService.processUploadedVideo(event);
            acknowledgment.acknowledge();
        } catch (Exception e) {
            log.error("Failed to process uploaded file {}", event.getFileId(), e);
            acknowledgment.acknowledge();
        }
    }
}