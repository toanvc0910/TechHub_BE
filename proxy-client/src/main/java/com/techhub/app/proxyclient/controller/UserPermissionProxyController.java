package com.techhub.app.proxyclient.controller;

import com.techhub.app.proxyclient.client.UserServiceClient;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/proxy/users/{userId}/permissions")
@RequiredArgsConstructor
public class UserPermissionProxyController {

    private final UserServiceClient userServiceClient;

    @GetMapping("/effective")
    public ResponseEntity<String> getEffective(@PathVariable String userId,
                                               @RequestHeader("Authorization") String authHeader) {
        return userServiceClient.getEffectivePermissions(userId, authHeader);
    }

    @PostMapping
    public ResponseEntity<String> upsertUserPermission(@PathVariable String userId,
                                                       @RequestBody Object body,
                                                       @RequestHeader("Authorization") String authHeader) {
        return userServiceClient.upsertUserPermission(userId, body, authHeader);
    }

    @DeleteMapping("/{permissionId}")
    public ResponseEntity<String> deactivateUserPermission(@PathVariable String userId,
                                                           @PathVariable String permissionId,
                                                           @RequestHeader("Authorization") String authHeader) {
        return userServiceClient.deactivateUserPermission(userId, permissionId, authHeader);
    }

    @PostMapping("/check")
    public ResponseEntity<String> checkPermission(@PathVariable String userId,
                                                  @RequestBody Object body,
                                                  @RequestHeader("Authorization") String authHeader) {
        return userServiceClient.checkPermission(userId, body, authHeader);
    }
}
