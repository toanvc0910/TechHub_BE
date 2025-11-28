package com.techhub.app.proxyclient.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@FeignClient(name = "NOTIFICATION-SERVICE")
public interface NotificationServiceClient {

    @GetMapping("/api/notifications")
    ResponseEntity<Object> getNotifications(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) Boolean read,
            @RequestHeader("Authorization") String authHeader);

    @GetMapping("/api/notifications/count/unread")
    ResponseEntity<Object> getUnreadCount(@RequestHeader("Authorization") String authHeader);

    @PutMapping("/api/notifications/{notificationId}/read")
    ResponseEntity<Object> markAsRead(
            @PathVariable String notificationId,
            @RequestHeader("Authorization") String authHeader);

    @PutMapping("/api/notifications/read")
    ResponseEntity<Object> markAllAsRead(@RequestHeader("Authorization") String authHeader);
}
