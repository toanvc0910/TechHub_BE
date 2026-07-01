package com.techhub.app.userservice.service;

import com.techhub.app.userservice.enums.InstructorApplicationAiStatus;
import com.techhub.app.userservice.repository.InstructorApplicationCertificateRepository;
import com.techhub.app.userservice.repository.InstructorApplicationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class ScanUpdater {

    private final InstructorApplicationRepository applicationRepository;
    private final InstructorApplicationCertificateRepository certificateRepository;

    @Transactional
    public void updateCv(UUID id, InstructorApplicationAiStatus status, String data, String error) {
        applicationRepository.updateCvResult(id, status, data, error);
    }

    @Transactional
    public void updateCccdFront(UUID id, InstructorApplicationAiStatus status, String data, String error) {
        applicationRepository.updateCccdFrontResult(id, status, data, error);
    }

    @Transactional
    public void updateCccdBack(UUID id, InstructorApplicationAiStatus status, String data, String error) {
        applicationRepository.updateCccdBackResult(id, status, data, error);
    }

    @Transactional
    public void updateCertificate(UUID id, InstructorApplicationAiStatus status, String data, String error) {
        certificateRepository.updateScanResult(id, status, data, error);
    }
}
