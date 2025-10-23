package com.techhub.app.notificationservice.controller;

import com.techhub.app.commonservice.context.UserContext;
import com.techhub.app.commonservice.exception.UnauthorizedException;
import com.techhub.app.notificationservice.dto.NotificationResponse;
import com.techhub.app.notificationservice.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping
    public ResponseEntity<Page<NotificationResponse>> getNotifications(
            @RequestParam(value = "read", required = false) Boolean read,
            @PageableDefault(size = 20, sort = "created", direction = org.springframework.data.domain.Sort.Direction.DESC) Pageable pageable) {
        UUID currentUserId = requireUser();
        Page<NotificationResponse> page = notificationService.getNotifications(currentUserId, read, pageable);
        return ResponseEntity.ok(page);
    }

    @PatchMapping("/{notificationId}/read")
    public ResponseEntity<NotificationResponse> markAsRead(@PathVariable UUID notificationId) {
        UUID currentUserId = requireUser();
        NotificationResponse response = notificationService.markAsRead(notificationId, currentUserId);
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/read")
    public ResponseEntity<Void> markAllAsRead() {
        UUID currentUserId = requireUser();
        notificationService.markAllAsRead(currentUserId);
        return ResponseEntity.noContent().build();
    }

    private UUID requireUser() {
        UUID userId = UserContext.getCurrentUserId();
        if (userId == null) {
            throw new UnauthorizedException("Authentication required");
        }
        return userId;
    }
}
