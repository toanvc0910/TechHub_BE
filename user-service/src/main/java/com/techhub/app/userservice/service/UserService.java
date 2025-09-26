package com.techhub.app.userservice.service;

import com.techhub.app.userservice.dto.request.ChangePasswordRequest;
import com.techhub.app.userservice.dto.request.CreateUserRequest;
import com.techhub.app.userservice.dto.request.ForgotPasswordRequest;
import com.techhub.app.userservice.dto.request.ResetPasswordRequest;
import com.techhub.app.userservice.dto.request.UpdateUserRequest;
import com.techhub.app.userservice.dto.response.UserResponse;
import com.techhub.app.userservice.entity.User;
import com.techhub.app.userservice.enums.UserStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface UserService {

    UserResponse createUser(CreateUserRequest request);

    UserResponse getUserById(UUID userId);

    UserResponse getUserByEmail(String email);

    UserResponse getUserByUsername(String username);

    UserResponse updateUser(UUID userId, UpdateUserRequest request);

    void deleteUser(UUID userId);

    void changePassword(UUID userId, ChangePasswordRequest request);

    void forgotPassword(ForgotPasswordRequest request);

    void resetPassword(String email, ResetPasswordRequest request);

    void activateUser(UUID userId);

    void deactivateUser(UUID userId);

    void changeUserStatus(UUID userId, UserStatus status);

    Page<UserResponse> getAllUsers(Pageable pageable);

    Page<UserResponse> getUsersByStatus(UserStatus status, Pageable pageable);

    Page<UserResponse> searchUsers(String keyword, Pageable pageable);

    boolean existsByEmail(String email);

    boolean existsByUsername(String username);

    long countUsersByStatus(UserStatus status);

    User getCurrentUser(String token);
}
