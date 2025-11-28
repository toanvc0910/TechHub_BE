package com.techhub.app.courseservice.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@FeignClient(name = "USER-SERVICE", path = "/api/users")
public interface UserServiceClient {

    /**
     * Get all active user IDs for broadcast notifications
     * Response format: { "data": [uuid1, uuid2, ...], "statusCode": 200, "message":
     * "..." }
     */
    @GetMapping("/internal/all-user-ids")
    Map<String, Object> getAllActiveUserIds();
}
