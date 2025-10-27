package com.techhub.app.fileservice.service;

import com.techhub.app.fileservice.dto.request.TrackFileUsageRequest;

import java.util.List;
import java.util.UUID;

public interface FileUsageService {
    void trackUsage(TrackFileUsageRequest request);

    void removeUsage(UUID fileId, String usedInType, UUID usedInId);

    void removeAllByUsedIn(String usedInType, UUID usedInId);

    List<?> listUsagesByFile(UUID fileId);
}
