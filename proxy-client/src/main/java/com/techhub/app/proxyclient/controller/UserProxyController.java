package com.techhub.app.proxyclient.controller;

import com.techhub.app.commonservice.exception.BadRequestException;
import com.techhub.app.commonservice.exception.UnauthorizedException;
import com.techhub.app.proxyclient.client.UserServiceClient;
import com.techhub.app.commonservice.jwt.JwtUtil;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;

@Slf4j
@RestController
@RequestMapping("/api/proxy/users")
@RequiredArgsConstructor
public class UserProxyController {

    private final UserServiceClient userServiceClient;
    private final JwtUtil jwtUtil;

    @GetMapping
    public ResponseEntity<String> getAllUsers(@RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String search,
            @RequestHeader("Authorization") String authHeader) {
        return userServiceClient.getAllUsers(page, size, search, authHeader);
    }

    @PostMapping
    public ResponseEntity<String> createUser(@RequestBody Object createUserRequest) {
        return userServiceClient.createUser(createUserRequest);
    }

    @GetMapping("/{userId}")
    public ResponseEntity<String> getUserById(@PathVariable String userId,
            @RequestHeader("Authorization") String authHeader) {
        return userServiceClient.getUserById(userId, authHeader);
    }

    @GetMapping("/email/{email}")
    public ResponseEntity<String> getUserByEmail(@PathVariable String email,
            @RequestHeader("Authorization") String authHeader) {
        return userServiceClient.getUserByEmail(email, authHeader);
    }

    @GetMapping("/username/{username}")
    public ResponseEntity<String> getUserByUsername(@PathVariable String username,
            @RequestHeader("Authorization") String authHeader) {
        return userServiceClient.getUserByUsername(username, authHeader);
    }

    @PutMapping("/{userId}")
    public ResponseEntity<String> updateUser(@PathVariable String userId,
            @RequestBody Object updateRequest,
            @RequestHeader("Authorization") String authHeader) {
        return userServiceClient.updateUser(userId, updateRequest, authHeader);
    }

    @DeleteMapping("/{userId}")
    public ResponseEntity<String> deleteUser(@PathVariable String userId,
            @RequestHeader("Authorization") String authHeader) {
        return userServiceClient.deleteUser(userId, authHeader);
    }

    // Password management endpoints
    @PostMapping("/change-password")
    public ResponseEntity<String> changePassword(
            @RequestBody Object changePasswordRequest,
            @RequestHeader("Authorization") String authHeader) {
        String token = authHeader.replace("Bearer ", "");
        UUID userId;
        try {
            userId = jwtUtil.getUserIdFromToken(token);
        } catch (RuntimeException e) {
            log.warn("Invalid token while processing change password", e);
            throw new UnauthorizedException("Invalid or expired token");
        }

        log.info("Change password request for user: {}", userId);
        return userServiceClient.changePassword(changePasswordRequest, userId.toString());
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<String> forgotPassword(@RequestBody Object forgotPasswordRequest) {
        return userServiceClient.forgotPassword(forgotPasswordRequest);
    }

    @PostMapping("/resend-reset-code/{email}")
    public ResponseEntity<String> resendResetCode(@PathVariable String email) {
        return userServiceClient.resendResetCode(email);
    }

    @PostMapping("/reset-password/{email}")
    public ResponseEntity<String> resetPassword(@PathVariable String email,
            @RequestBody Object resetPasswordRequest) {
        return userServiceClient.resetPassword(email, resetPasswordRequest);
    }

    // User status management endpoints
    @PostMapping("/{userId}/activate")
    public ResponseEntity<String> activateUser(@PathVariable String userId,
            @RequestHeader("Authorization") String authHeader) {
        return userServiceClient.activateUser(userId, authHeader);
    }

    @PostMapping("/{userId}/deactivate")
    public ResponseEntity<String> deactivateUser(@PathVariable String userId,
            @RequestHeader("Authorization") String authHeader) {
        return userServiceClient.deactivateUser(userId, authHeader);
    }

    @PutMapping("/{userId}/status/{status}")
    public ResponseEntity<String> changeUserStatus(@PathVariable String userId,
            @PathVariable String status,
            @RequestHeader("Authorization") String authHeader) {
        return userServiceClient.changeUserStatus(userId, status, authHeader);
    }

    // Profile endpoint
    @GetMapping("/profile")
    public ResponseEntity<String> getCurrentUserProfile(@RequestHeader("Authorization") String authHeader,
            HttpServletRequest request) {
        // Get user info from request attributes (set by JwtAuthenticationFilter)
        Object userId = request.getAttribute("userId");
        Object userEmail = request.getAttribute("userEmail");

        if (userId == null || userEmail == null) {
            throw new BadRequestException("User context missing");
        }

        return userServiceClient.getCurrentUserProfile(authHeader, userId.toString(), userEmail.toString());
    }

    // Public endpoints - no authentication required
    @GetMapping("/public/instructors")
    public ResponseEntity<String> getPublicInstructors(@RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "4") int size) {
        return userServiceClient.getPublicInstructors(page, size);
    }

    // ===== Instructor Applications =====
    @PostMapping("/instructor-applications")
    public ResponseEntity<String> submitInstructorApplication(@RequestBody Object body,
            HttpServletRequest httpReq) {
        Object uid = httpReq.getAttribute("userId");
        if (uid == null) throw new UnauthorizedException("User context missing");
        return userServiceClient.submitInstructorApplication(body, uid.toString());
    }

    @GetMapping("/instructor-applications/me")
    public ResponseEntity<String> getMyInstructorApplications(HttpServletRequest httpReq) {
        Object uid = httpReq.getAttribute("userId");
        if (uid == null) throw new UnauthorizedException("User context missing");
        return userServiceClient.getMyInstructorApplications(uid.toString());
    }

    @GetMapping("/instructor-applications")
    public ResponseEntity<String> listInstructorApplications(
            @RequestParam(value = "status", required = false) String status,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "10") int size,
            HttpServletRequest httpReq) {
        Object roles = httpReq.getAttribute("userRoles");
        return userServiceClient.listInstructorApplications(status, page, size,
                roles == null ? "" : roles.toString());
    }

    @GetMapping("/instructor-applications/{id}")
    public ResponseEntity<String> getInstructorApplicationDetail(@PathVariable String id,
            HttpServletRequest httpReq) {
        Object uid = httpReq.getAttribute("userId");
        Object roles = httpReq.getAttribute("userRoles");
        if (uid == null) throw new UnauthorizedException("User context missing");
        return userServiceClient.getInstructorApplicationDetail(id, uid.toString(),
                roles == null ? "" : roles.toString());
    }

    @PutMapping("/instructor-applications/{id}/approve")
    public ResponseEntity<String> approveInstructorApplication(@PathVariable String id,
            @RequestBody(required = false) Object body, HttpServletRequest httpReq) {
        Object uid = httpReq.getAttribute("userId");
        Object roles = httpReq.getAttribute("userRoles");
        if (uid == null) throw new UnauthorizedException("User context missing");
        return userServiceClient.approveInstructorApplication(id, body, uid.toString(),
                roles == null ? "" : roles.toString());
    }

    @PutMapping("/instructor-applications/{id}/reject")
    public ResponseEntity<String> rejectInstructorApplication(@PathVariable String id,
            @RequestBody Object body, HttpServletRequest httpReq) {
        Object uid = httpReq.getAttribute("userId");
        Object roles = httpReq.getAttribute("userRoles");
        if (uid == null) throw new UnauthorizedException("User context missing");
        return userServiceClient.rejectInstructorApplication(id, body, uid.toString(),
                roles == null ? "" : roles.toString());
    }

    @PostMapping("/instructor-applications/{id}/rescan/cv")
    public ResponseEntity<String> rescanCv(@PathVariable String id, HttpServletRequest httpReq) {
        Object roles = httpReq.getAttribute("userRoles");
        return userServiceClient.rescanCv(id, roles == null ? "" : roles.toString());
    }

    @PostMapping("/instructor-applications/{id}/rescan/cccd-front")
    public ResponseEntity<String> rescanCccdFront(@PathVariable String id, HttpServletRequest httpReq) {
        Object roles = httpReq.getAttribute("userRoles");
        return userServiceClient.rescanCccdFront(id, roles == null ? "" : roles.toString());
    }

    @PostMapping("/instructor-applications/{id}/rescan/cccd-back")
    public ResponseEntity<String> rescanCccdBack(@PathVariable String id, HttpServletRequest httpReq) {
        Object roles = httpReq.getAttribute("userRoles");
        return userServiceClient.rescanCccdBack(id, roles == null ? "" : roles.toString());
    }

    @PostMapping("/instructor-applications/certificates/{certId}/rescan")
    public ResponseEntity<String> rescanCertificate(@PathVariable String certId, HttpServletRequest httpReq) {
        Object roles = httpReq.getAttribute("userRoles");
        return userServiceClient.rescanCertificate(certId, roles == null ? "" : roles.toString());
    }

    @GetMapping("/instructor-profiles/me")
    public ResponseEntity<String> getMyInstructorProfile(HttpServletRequest httpReq) {
        Object uid = httpReq.getAttribute("userId");
        if (uid == null) throw new UnauthorizedException("User context missing");
        return userServiceClient.getMyInstructorProfile(uid.toString());
    }

    @GetMapping("/instructor-profiles/{userId}")
    public ResponseEntity<String> getInstructorProfile(@PathVariable String userId, HttpServletRequest httpReq) {
        Object roles = httpReq.getAttribute("userRoles");
        Object callerId = httpReq.getAttribute("userId");
        return userServiceClient.getInstructorProfile(userId,
                callerId == null ? "" : callerId.toString(),
                roles == null ? "" : roles.toString());
    }
}
