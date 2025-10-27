package com.techhub.app.fileservice.service.impl;

import com.techhub.app.fileservice.dto.request.TrackFileUsageRequest;
import com.techhub.app.fileservice.entity.FileEntity;
import com.techhub.app.fileservice.entity.FileUsageEntity;
import com.techhub.app.fileservice.repository.FileRepository;
import com.techhub.app.fileservice.repository.FileUsageRepository;
import com.techhub.app.fileservice.service.FileUsageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class FileUsageServiceImpl implements FileUsageService {

    private final FileUsageRepository usageRepository;
    private final FileRepository fileRepository;

    @Override
    public void trackUsage(TrackFileUsageRequest request) {
        // Validate file exists
        FileEntity file = fileRepository.findById(request.getFileId())
                .orElseThrow(() -> new RuntimeException("File not found"));

        // Check if usage already exists
        if (usageRepository.existsByFileIdAndUsedInTypeAndUsedInId(
                request.getFileId(), request.getUsedInType(), request.getUsedInId())) {
            return;
        }

        FileUsageEntity usage = new FileUsageEntity();
        usage.setFileId(request.getFileId());
        usage.setUsedInType(request.getUsedInType());
        usage.setUsedInId(request.getUsedInId());

        FileUsageEntity saved = usageRepository.save(usage);
        log.debug("Tracked usage {} for file {}", saved.getId(), file.getId());
    }

    @Override
    public void removeUsage(UUID fileId, String usedInType, UUID usedInId) {
        usageRepository.findByFileIdAndUsedInTypeAndUsedInId(fileId, usedInType, usedInId)
                .ifPresent(usage -> {
                    usageRepository.delete(usage);
                    log.debug("Removed usage for file {}", fileId);
                });
    }

    @Override
    public void removeAllByUsedIn(String usedInType, UUID usedInId) {
        usageRepository.deleteByUsedInTypeAndUsedInId(usedInType, usedInId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<?> listUsagesByFile(UUID fileId) {
        return usageRepository.findByFileId(fileId)
                .stream()
                .map(u -> {
                    return new Object() {
                        public final UUID id = u.getId();
                        public final UUID fileId = u.getFileId();
                        public final UUID usedInId = u.getUsedInId();
                        public final String usedInType = u.getUsedInType();
                        public final LocalDateTime created = u.getCreated();
                    };
                })
                .collect(Collectors.toList());
    }
}
