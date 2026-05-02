package com.techhub.app.userservice.service;

import com.techhub.app.userservice.dto.request.CreateInstructorApplicationRequest;
import com.techhub.app.userservice.dto.request.ReviewInstructorApplicationRequest;
import com.techhub.app.userservice.dto.response.InstructorApplicationResponse;
import com.techhub.app.userservice.entity.InstructorApplication;
import com.techhub.app.userservice.entity.Role;
import com.techhub.app.userservice.entity.User;
import com.techhub.app.userservice.entity.UserRole;
import com.techhub.app.userservice.enums.InstructorApplicationAdminStatus;
import com.techhub.app.userservice.enums.InstructorApplicationAiStatus;
import com.techhub.app.userservice.repository.InstructorApplicationRepository;
import com.techhub.app.userservice.repository.RoleRepository;
import com.techhub.app.userservice.repository.UserRepository;
import com.techhub.app.userservice.repository.UserRoleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class InstructorApplicationService {

    private final InstructorApplicationRepository applicationRepository;
    private final UserRepository userRepository;
    private final UserRoleRepository userRoleRepository;
    private final RoleRepository roleRepository;
    private final N8nCvScanClient n8nClient;

    @Transactional
    public InstructorApplicationResponse submit(UUID userId, CreateInstructorApplicationRequest request) {
        // Block nếu đã APPROVED (đã là instructor).
        Optional<InstructorApplication> approved = applicationRepository
                .findFirstByUserIdAndAdminStatusAndIsActiveTrueOrderByCreatedDesc(
                        userId, InstructorApplicationAdminStatus.APPROVED);
        if (approved.isPresent()) {
            throw new IllegalArgumentException("Bạn đã được duyệt làm giảng viên");
        }
        // Block nếu đang có app PENDING + AI đã PROCESSED (đợi admin duyệt).
        // Nếu AI FAILED hoặc PENDING quá lâu → cho nộp lại, đánh dấu đơn cũ inactive.
        Optional<InstructorApplication> existingPending = applicationRepository
                .findFirstByUserIdAndAdminStatusAndIsActiveTrueOrderByCreatedDesc(
                        userId, InstructorApplicationAdminStatus.PENDING);
        if (existingPending.isPresent()) {
            InstructorApplication oldApp = existingPending.get();
            if (oldApp.getAiStatus() == InstructorApplicationAiStatus.PROCESSED) {
                throw new IllegalArgumentException("Bạn đang có đơn ứng tuyển chờ admin duyệt");
            }
            // AI failed hoặc đang pending - cho phép thay thế.
            oldApp.setIsActive(false);
            applicationRepository.save(oldApp);
            log.info("[InstructorApp] Soft-deleted old failed/pending app id={} for userId={}",
                    oldApp.getId(), userId);
        }

        InstructorApplication app = InstructorApplication.builder()
                .userId(userId)
                .cvFileId(request.getCvFileId())
                .cvFileUrl(request.getCvFileUrl())
                .aiStatus(InstructorApplicationAiStatus.PENDING)
                .adminStatus(InstructorApplicationAdminStatus.PENDING)
                .isActive(true)
                .build();
        InstructorApplication saved = applicationRepository.save(app);
        log.info("[InstructorApp] Created applicationId={} userId={}", saved.getId(), userId);

        // Async fire N8n
        n8nClient.triggerCvScan(saved.getId(), saved.getCvFileUrl());
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<InstructorApplicationResponse> getMyApplications(UUID userId) {
        return applicationRepository.findByUserIdAndIsActiveTrueOrderByCreatedDesc(userId)
                .stream().map(this::toResponse).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public Page<InstructorApplicationResponse> listForAdmin(
            InstructorApplicationAdminStatus status, int page, int size) {
        Pageable pageable = PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 100));
        Page<InstructorApplication> rows = status == null
                ? applicationRepository.findByIsActiveTrueOrderByCreatedDesc(pageable)
                : applicationRepository.findByAdminStatusAndIsActiveTrueOrderByCreatedDesc(status, pageable);
        return rows.map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public InstructorApplicationResponse getDetail(UUID applicationId) {
        return applicationRepository.findById(applicationId)
                .map(this::toResponse)
                .orElseThrow(() -> new IllegalArgumentException("Application not found"));
    }

    @Transactional
    public InstructorApplicationResponse approve(UUID applicationId, UUID reviewerId,
            ReviewInstructorApplicationRequest request) {
        InstructorApplication app = applicationRepository.findById(applicationId)
                .orElseThrow(() -> new IllegalArgumentException("Application not found"));
        if (app.getAdminStatus() != InstructorApplicationAdminStatus.PENDING) {
            throw new IllegalArgumentException("Đơn này đã được xử lý");
        }
        app.setAdminStatus(InstructorApplicationAdminStatus.APPROVED);
        app.setAdminNote(request == null ? null : request.getNote());
        app.setReviewedBy(reviewerId);
        app.setReviewedAt(LocalDateTime.now());
        applicationRepository.save(app);

        // Gán role INSTRUCTOR (giữ LEARNER).
        Role instructorRole = roleRepository.findByName("INSTRUCTOR")
                .orElseThrow(() -> new IllegalStateException("INSTRUCTOR role not found"));
        if (!userRoleRepository.existsByUserIdAndRoleId(app.getUserId(), instructorRole.getId())) {
            UserRole ur = new UserRole();
            ur.setUserId(app.getUserId());
            ur.setRoleId(instructorRole.getId());
            ur.setAssignedAt(LocalDateTime.now());
            ur.setCreatedBy(reviewerId);
            ur.setUpdatedBy(reviewerId);
            ur.setIsActive(true);
            userRoleRepository.save(ur);
            log.info("[InstructorApp] Granted INSTRUCTOR role to userId={}", app.getUserId());
        }
        return toResponse(app);
    }

    @Transactional
    public InstructorApplicationResponse reject(UUID applicationId, UUID reviewerId,
            ReviewInstructorApplicationRequest request) {
        InstructorApplication app = applicationRepository.findById(applicationId)
                .orElseThrow(() -> new IllegalArgumentException("Application not found"));
        if (app.getAdminStatus() != InstructorApplicationAdminStatus.PENDING) {
            throw new IllegalArgumentException("Đơn này đã được xử lý");
        }
        if (request == null || request.getNote() == null || request.getNote().isBlank()) {
            throw new IllegalArgumentException("Reject phải có lý do");
        }
        app.setAdminStatus(InstructorApplicationAdminStatus.REJECTED);
        app.setAdminNote(request.getNote());
        app.setReviewedBy(reviewerId);
        app.setReviewedAt(LocalDateTime.now());
        applicationRepository.save(app);
        return toResponse(app);
    }

    private InstructorApplicationResponse toResponse(InstructorApplication app) {
        String userName = null;
        String userEmail = null;
        Optional<User> userOpt = userRepository.findById(app.getUserId());
        if (userOpt.isPresent()) {
            userName = userOpt.get().getUsername();
            userEmail = userOpt.get().getEmail();
        }
        return InstructorApplicationResponse.builder()
                .id(app.getId())
                .userId(app.getUserId())
                .userName(userName)
                .userEmail(userEmail)
                .cvFileId(app.getCvFileId())
                .cvFileUrl(app.getCvFileUrl())
                .aiStatus(app.getAiStatus() == null ? null : app.getAiStatus().name())
                .aiExtractedData(app.getAiExtractedData())
                .aiError(app.getAiError())
                .adminStatus(app.getAdminStatus() == null ? null : app.getAdminStatus().name())
                .adminNote(app.getAdminNote())
                .reviewedBy(app.getReviewedBy())
                .reviewedAt(app.getReviewedAt())
                .created(app.getCreated())
                .updated(app.getUpdated())
                .build();
    }
}
