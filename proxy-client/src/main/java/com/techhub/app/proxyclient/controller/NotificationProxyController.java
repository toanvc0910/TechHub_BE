package com.techhub.app.proxyclient.controller;

import com.techhub.app.proxyclient.client.NotificationServiceClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/proxy/notifications")
@RequiredArgsConstructor
public class NotificationProxyController {

    private final NotificationServiceClient notificationServiceClient;

    @GetMapping
    public ResponseEntity<Object> getNotifications(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) Boolean read,
            @RequestHeader("Authorization") String authHeader) {
        log.debug("🔔 [PROXY] GET /notifications - page: {}, size: {}, read: {}", page, size, read);
        ResponseEntity<Object> response = notificationServiceClient.getNotifications(page, size, read, authHeader);
        log.debug("🔔 [PROXY] GET /notifications - response status: {}", response.getStatusCode());
        return response;
    }

    @GetMapping("/count/unread")
    public ResponseEntity<Object> getUnreadCount(@RequestHeader("Authorization") String authHeader) {
        log.debug("🔔 [PROXY] GET /notifications/count/unread");
        ResponseEntity<Object> response = notificationServiceClient.getUnreadCount(authHeader);
        log.debug("🔔 [PROXY] GET /notifications/count/unread - response: {}", response.getBody());
        return response;
    }

    @PutMapping("/{notificationId}/read")
    public ResponseEntity<Object> markAsRead(
            @PathVariable String notificationId,
            @RequestHeader("Authorization") String authHeader) {
        log.debug("🔔 [PROXY] PUT /notifications/{}/read", notificationId);
        return notificationServiceClient.markAsRead(notificationId, authHeader);
    }

    @PutMapping("/read")
    public ResponseEntity<Object> markAllAsRead(@RequestHeader("Authorization") String authHeader) {
        log.debug("🔔 [PROXY] PUT /notifications/read - mark all as read");
        return notificationServiceClient.markAllAsRead(authHeader);
    }
}
