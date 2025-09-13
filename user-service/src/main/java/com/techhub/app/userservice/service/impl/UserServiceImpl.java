package com.techhub.app.userservice.service.impl;

import com.techhub.app.userservice.dto.request.ChangePasswordRequest;
import com.techhub.app.userservice.dto.request.CreateUserRequest;
import com.techhub.app.userservice.dto.request.ForgotPasswordRequest;
import com.techhub.app.userservice.dto.request.ResetPasswordRequest;
import com.techhub.app.userservice.dto.request.UpdateUserRequest;
import com.techhub.app.userservice.dto.response.UserResponse;
import com.techhub.app.userservice.entity.Profile;
import com.techhub.app.userservice.entity.User;
import com.techhub.app.userservice.enums.Language;
import com.techhub.app.userservice.enums.OtpType;
import com.techhub.app.userservice.enums.UserRole;
import com.techhub.app.userservice.enums.UserStatus;
import com.techhub.app.userservice.repository.ProfileRepository;
import com.techhub.app.userservice.repository.UserRepository;
import com.techhub.app.userservice.service.EmailService;
import com.techhub.app.userservice.service.OTPService;
import com.techhub.app.userservice.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final ProfileRepository profileRepository;
    private final PasswordEncoder passwordEncoder;
    private final OTPService otpService;
    private final EmailService emailService;

    @Override
    public UserResponse createUser(CreateUserRequest request) {
        log.info("Creating user with email: {}", request.getEmail());

        // Validate email and username uniqueness
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("Email already exists");
        }

        if (userRepository.existsByUsername(request.getUsername())) {
            throw new RuntimeException("Username already exists");
        }

        // Create user
        User user = new User();
        user.setEmail(request.getEmail());
        user.setUsername(request.getUsername());
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setRole(UserRole.USER);
        user.setStatus(UserStatus.PENDING);

        User savedUser = userRepository.save(user);

        // Create profile
        Profile profile = new Profile();
        profile.setUser(savedUser);
        profile.setFullName(request.getFullName());
        profile.setPreferredLanguage(Language.VI);
        profileRepository.save(profile);

        // Send welcome email
        emailService.sendWelcomeEmail(savedUser.getEmail(), savedUser.getUsername());

        log.info("User created successfully with ID: {}", savedUser.getId());
        return mapToUserResponse(savedUser, profile);
    }

    @Override
    @Transactional(readOnly = true)
    public UserResponse getUserById(UUID userId) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new RuntimeException("User not found"));

        Profile profile = profileRepository.findByUserId(userId).orElse(null);
        return mapToUserResponse(user, profile);
    }

    @Override
    @Transactional(readOnly = true)
    public UserResponse getUserByEmail(String email) {
        User user = userRepository.findByEmailAndIsActiveTrue(email)
            .orElseThrow(() -> new RuntimeException("User not found"));

        Profile profile = profileRepository.findByUserId(user.getId()).orElse(null);
        return mapToUserResponse(user, profile);
    }

    @Override
    @Transactional(readOnly = true)
    public UserResponse getUserByUsername(String username) {
        User user = userRepository.findByUsernameAndIsActiveTrue(username)
            .orElseThrow(() -> new RuntimeException("User not found"));

        Profile profile = profileRepository.findByUserId(user.getId()).orElse(null);
        return mapToUserResponse(user, profile);
    }

    @Override
    public UserResponse updateUser(UUID userId, UpdateUserRequest request) {
        log.info("Updating user with ID: {}", userId);

        User user = userRepository.findById(userId)
            .orElseThrow(() -> new RuntimeException("User not found"));

        // Update user fields
        if (request.getEmail() != null && !request.getEmail().equals(user.getEmail())) {
            if (userRepository.existsByEmail(request.getEmail())) {
                throw new RuntimeException("Email already exists");
            }
            user.setEmail(request.getEmail());
        }

        if (request.getUsername() != null && !request.getUsername().equals(user.getUsername())) {
            if (userRepository.existsByUsername(request.getUsername())) {
                throw new RuntimeException("Username already exists");
            }
            user.setUsername(request.getUsername());
        }

        User savedUser = userRepository.save(user);

        // Update profile
        Profile profile = profileRepository.findByUserId(userId)
            .orElseGet(() -> {
                Profile newProfile = new Profile();
                newProfile.setUser(savedUser);
                return newProfile;
            });

        if (request.getFullName() != null) {
            profile.setFullName(request.getFullName());
        }
        if (request.getBio() != null) {
            profile.setBio(request.getBio());
        }
        if (request.getLocation() != null) {
            profile.setLocation(request.getLocation());
        }
        if (request.getAvatarUrl() != null) {
            profile.setAvatarUrl(request.getAvatarUrl());
        }
        if (request.getPreferredLanguage() != null) {
            profile.setPreferredLanguage(Language.valueOf(request.getPreferredLanguage()));
        }

        Profile savedProfile = profileRepository.save(profile);

        log.info("User updated successfully with ID: {}", userId);
        return mapToUserResponse(savedUser, savedProfile);
    }

    @Override
    public void deleteUser(UUID userId) {
        log.info("Deleting user with ID: {}", userId);

        User user = userRepository.findById(userId)
            .orElseThrow(() -> new RuntimeException("User not found"));

        user.setIsActive(false);
        user.setStatus(UserStatus.DELETED);
        userRepository.save(user);

        log.info("User deleted successfully with ID: {}", userId);
    }

    @Override
    public void changePassword(UUID userId, ChangePasswordRequest request) {
        log.info("Changing password for user ID: {}", userId);

        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            throw new RuntimeException("New password and confirm password do not match");
        }

        User user = userRepository.findById(userId)
            .orElseThrow(() -> new RuntimeException("User not found"));

        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPasswordHash())) {
            throw new RuntimeException("Current password is incorrect");
        }

        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);

        log.info("Password changed successfully for user ID: {}", userId);
    }

    @Override
    public void forgotPassword(ForgotPasswordRequest request) {
        log.info("Processing forgot password request for email: {}", request.getEmail());

        User user = userRepository.findByEmailAndIsActiveTrue(request.getEmail())
            .orElseThrow(() -> new RuntimeException("User not found"));

        // Generate OTP
        String otpCode = otpService.generateOTP(user.getId(), OtpType.PASSWORD_RESET);

        // Send reset email
        emailService.sendPasswordResetEmail(user.getEmail(), otpCode);

        log.info("Password reset email sent for user: {}", request.getEmail());
    }

    @Override
    public void resetPassword(String email, ResetPasswordRequest request) {
        log.info("Processing password reset for email: {}", email);

        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            throw new RuntimeException("New password and confirm password do not match");
        }

        User user = userRepository.findByEmailAndIsActiveTrue(email)
            .orElseThrow(() -> new RuntimeException("User not found"));

        // Validate OTP
        if (!otpService.validateOTPForUser(user.getId(), request.getOtpCode(), OtpType.PASSWORD_RESET)) {
            throw new RuntimeException("Invalid or expired OTP");
        }

        // Update password
        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);

        // Mark OTP as used
        otpService.markOTPAsUsed(request.getOtpCode(), OtpType.PASSWORD_RESET);

        log.info("Password reset successfully for user: {}", email);
    }

    @Override
    public void activateUser(UUID userId) {
        log.info("Activating user with ID: {}", userId);

        User user = userRepository.findById(userId)
            .orElseThrow(() -> new RuntimeException("User not found"));

        user.setStatus(UserStatus.ACTIVE);
        user.setIsActive(true);
        userRepository.save(user);

        emailService.sendAccountActivationEmail(user.getEmail(), user.getUsername());

        log.info("User activated successfully with ID: {}", userId);
    }

    @Override
    public void deactivateUser(UUID userId) {
        log.info("Deactivating user with ID: {}", userId);

        User user = userRepository.findById(userId)
            .orElseThrow(() -> new RuntimeException("User not found"));

        user.setStatus(UserStatus.INACTIVE);
        userRepository.save(user);

        log.info("User deactivated successfully with ID: {}", userId);
    }

    @Override
    public void changeUserStatus(UUID userId, UserStatus status) {
        log.info("Changing status for user ID: {} to {}", userId, status);

        User user = userRepository.findById(userId)
            .orElseThrow(() -> new RuntimeException("User not found"));

        user.setStatus(status);
        userRepository.save(user);

        log.info("User status changed successfully for ID: {}", userId);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<UserResponse> getAllUsers(Pageable pageable) {
        Page<User> users = userRepository.findByIsActiveTrueOrderByCreatedDesc(pageable);
        return users.map(user -> {
            Profile profile = profileRepository.findByUserId(user.getId()).orElse(null);
            return mapToUserResponse(user, profile);
        });
    }

    @Override
    @Transactional(readOnly = true)
    public Page<UserResponse> getUsersByStatus(UserStatus status, Pageable pageable) {
        Page<User> users = userRepository.findByStatusAndIsActiveTrueOrderByCreatedDesc(status, pageable);
        return users.map(user -> {
            Profile profile = profileRepository.findByUserId(user.getId()).orElse(null);
            return mapToUserResponse(user, profile);
        });
    }

    @Override
    @Transactional(readOnly = true)
    public Page<UserResponse> searchUsers(String keyword, Pageable pageable) {
        Page<User> users = userRepository.searchUsers(keyword, pageable);
        return users.map(user -> {
            Profile profile = profileRepository.findByUserId(user.getId()).orElse(null);
            return mapToUserResponse(user, profile);
        });
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsByEmail(String email) {
        return userRepository.existsByEmail(email);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsByUsername(String username) {
        return userRepository.existsByUsername(username);
    }

    @Override
    @Transactional(readOnly = true)
    public long countUsersByStatus(UserStatus status) {
        return userRepository.countByStatusAndIsActiveTrue(status);
    }

    private UserResponse mapToUserResponse(User user, Profile profile) {
        UserResponse response = new UserResponse();
        response.setId(user.getId());
        response.setEmail(user.getEmail());
        response.setUsername(user.getUsername());
        response.setRole(user.getRole());
        response.setStatus(user.getStatus());
        response.setCreated(user.getCreated());
        response.setUpdated(user.getUpdated());

        if (profile != null) {
            response.setFullName(profile.getFullName());
            response.setAvatarUrl(profile.getAvatarUrl());
            response.setBio(profile.getBio());
            response.setLocation(profile.getLocation());
            response.setPreferredLanguage(profile.getPreferredLanguage());
        }

        return response;
    }
}
