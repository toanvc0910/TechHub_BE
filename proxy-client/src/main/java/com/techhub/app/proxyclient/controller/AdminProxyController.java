package com.techhub.app.proxyclient.controller;

import com.techhub.app.proxyclient.client.UserServiceClient;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/proxy/admin")
@RequiredArgsConstructor
public class AdminProxyController {

    private final UserServiceClient userServiceClient;

    @GetMapping("/permissions")
    public ResponseEntity<String> listPermissions(@RequestHeader("Authorization") String authHeader) {
        return userServiceClient.listPermissions(authHeader);
    }

    @PostMapping("/permissions")
    public ResponseEntity<String> createPermission(@RequestBody Object body,
                                                   @RequestHeader("Authorization") String authHeader) {
        return userServiceClient.createPermission(body, authHeader);
    }

    @PutMapping("/permissions/{permissionId}")
    public ResponseEntity<String> updatePermission(@PathVariable String permissionId,
                                                   @RequestBody Object body,
                                                   @RequestHeader("Authorization") String authHeader) {
        return userServiceClient.updatePermission(permissionId, body, authHeader);
    }

    @GetMapping("/roles")
    public ResponseEntity<String> listRoles(@RequestHeader("Authorization") String authHeader) {
        return userServiceClient.listRoles(authHeader);
    }

    @PostMapping("/roles")
    public ResponseEntity<String> createRole(@RequestBody Object body,
                                             @RequestHeader("Authorization") String authHeader) {
        return userServiceClient.createRole(body, authHeader);
    }

    @PutMapping("/roles/{roleId}")
    public ResponseEntity<String> updateRole(@PathVariable String roleId,
                                             @RequestBody Object body,
                                             @RequestHeader("Authorization") String authHeader) {
        return userServiceClient.updateRole(roleId, body, authHeader);
    }

    @PostMapping("/roles/{roleId}/permissions")
    public ResponseEntity<String> assignPermissionsToRole(@PathVariable String roleId,
                                                          @RequestBody Object body,
                                                          @RequestHeader("Authorization") String authHeader) {
        return userServiceClient.assignPermissionsToRole(roleId, body, authHeader);
    }

    @DeleteMapping("/roles/{roleId}/permissions/{permissionId}")
    public ResponseEntity<String> removePermissionFromRole(@PathVariable String roleId,
                                                           @PathVariable String permissionId,
                                                           @RequestHeader("Authorization") String authHeader) {
        return userServiceClient.removePermissionFromRole(roleId, permissionId, authHeader);
    }

    @GetMapping("/users/{userId}/roles")
    public ResponseEntity<String> getUserRoles(@PathVariable String userId,
                                               @RequestHeader("Authorization") String authHeader) {
        return userServiceClient.getUserRoles(userId, authHeader);
    }

    @PostMapping("/users/{userId}/roles")
    public ResponseEntity<String> assignRolesToUser(@PathVariable String userId,
                                                    @RequestBody Object body,
                                                    @RequestHeader("Authorization") String authHeader) {
        return userServiceClient.assignRolesToUser(userId, body, authHeader);
    }

    @DeleteMapping("/users/{userId}/roles/{roleId}")
    public ResponseEntity<String> removeRoleFromUser(@PathVariable String userId,
                                                     @PathVariable String roleId,
                                                     @RequestHeader("Authorization") String authHeader) {
        return userServiceClient.removeRoleFromUser(userId, roleId, authHeader);
    }
}
