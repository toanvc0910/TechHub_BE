package com.techhub.app.fileservice.service;

import com.techhub.app.fileservice.kafka.FileUploadedEvent;

public interface MediaProcessingService {

    void processUploadedVideo(FileUploadedEvent event);
}