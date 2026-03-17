package com.techhub.app.notificationservice.controller;

import com.techhub.app.commonservice.context.UserContext;
import com.techhub.app.commonservice.exception.UnauthorizedException;
import com.techhub.app.commonservice.payload.GlobalResponse;
import com.techhub.app.commonservice.payload.PageGlobalResponse;
import com.techhub.app.notificationservice.dto.NotificationResponse;
import com.techhub.app.notificationservice.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.util.UUID;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping
    public ResponseEntity<PageGlobalResponse<NotificationResponse>> getNotifications(
            @RequestParam(value = "read", required = false) Boolean read,
            @PageableDefault(size = 20, sort = "created", direction = org.springframework.data.domain.Sort.Direction.DESC) Pageable pageable,
            HttpServletRequest request) {
        UUID currentUserId = requireUser();
        Page<NotificationResponse> page = notificationService.getNotifications(currentUserId, read, pageable);

        PageGlobalResponse.PaginationInfo paginationInfo = PageGlobalResponse.PaginationInfo.builder()
                .page(page.getNumber())
                .size(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .first(page.isFirst())
                .last(page.isLast())
                .hasNext(page.hasNext())
                .hasPrevious(page.hasPrevious())
                .build();

        return ResponseEntity.ok(
                PageGlobalResponse.success("Notifications retrieved successfully", page.getContent(), paginationInfo)
                        .withPath(request.getRequestURI()));
    }

    @PutMapping("/{notificationId}/read")
    public ResponseEntity<GlobalResponse<NotificationResponse>> markAsRead(@PathVariable UUID notificationId,
            HttpServletRequest request) {
        UUID currentUserId = requireUser();
        NotificationResponse response = notificationService.markAsRead(notificationId, currentUserId);
        return ResponseEntity.ok(
                GlobalResponse.success("Notification marked as read", response)
                        .withPath(request.getRequestURI()));
    }

    @PutMapping("/read")
    public ResponseEntity<GlobalResponse<Void>> markAllAsRead(HttpServletRequest request) {
        UUID currentUserId = requireUser();
        notificationService.markAllAsRead(currentUserId);
        return ResponseEntity.ok(
                GlobalResponse.<Void>success("All notifications marked as read", null)
                        .withPath(request.getRequestURI()));
    }

    @GetMapping("/count/unread")
    public ResponseEntity<GlobalResponse<Long>> countUnread(HttpServletRequest request) {
        UUID currentUserId = requireUser();
        long count = notificationService.countUnread(currentUserId);
        return ResponseEntity.ok(
                GlobalResponse.success("Unread notification count retrieved", count)
                        .withPath(request.getRequestURI()));
    }

    private UUID requireUser() {
        UUID userId = UserContext.getCurrentUserId();
        if (userId == null) {
            throw new UnauthorizedException("Authentication required");
        }
        return userId;
    }
}
