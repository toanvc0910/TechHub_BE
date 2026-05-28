package com.techhub.app.userservice.controller;

import com.techhub.app.commonservice.payload.GlobalResponse;
import com.techhub.app.userservice.dto.response.InstructorProfileResponse;
import com.techhub.app.userservice.service.InstructorProfileSyncService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/instructor-profiles")
@RequiredArgsConstructor
@Slf4j
public class InstructorProfileController {

    private final InstructorProfileSyncService profileService;

    @GetMapping("/me")
    public ResponseEntity<GlobalResponse<InstructorProfileResponse>> getMine(
            @RequestHeader(value = "X-User-Id", required = false) String userId) {
        try {
            UUID uid = UUID.fromString(userId);
            Optional<InstructorProfileResponse> profile = profileService.getProfile(uid, true);
            return profile
                    .map(p -> ResponseEntity.ok(GlobalResponse.success("Instructor profile", p)))
                    .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND)
                            .body(GlobalResponse.error("Chưa có profile giảng viên", HttpStatus.NOT_FOUND.value())));
        } catch (Exception ex) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(GlobalResponse.error("Invalid user id", HttpStatus.BAD_REQUEST.value()));
        }
    }

    @GetMapping("/{userId}")
    public ResponseEntity<GlobalResponse<InstructorProfileResponse>> getByUserId(
            @RequestHeader(value = "X-User-Roles", required = false) String roles,
            @RequestHeader(value = "X-User-Id", required = false) String callerId,
            @PathVariable UUID userId) {
        boolean isAdmin = roles != null && roles.toUpperCase().contains("ADMIN");
        boolean isOwner = callerId != null && callerId.equals(userId.toString());
        boolean includeSensitive = isAdmin || isOwner;
        Optional<InstructorProfileResponse> profile = profileService.getProfile(userId, includeSensitive);
        return profile
                .map(p -> ResponseEntity.ok(GlobalResponse.success("Instructor profile", p)))
                .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(GlobalResponse.error("Không tìm thấy profile giảng viên", HttpStatus.NOT_FOUND.value())));
    }
}
