package com.techhub.app.courseservice.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

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

    /**
     * Batch lookup minimal user info {id, username, avatar} for given ids.
     * Response format: { "data": [{id,username,avatar}, ...], "statusCode": 200 }
     */
    @PostMapping("/internal/batch")
    Map<String, Object> getUsersBatch(@RequestBody List<UUID> ids);
}
