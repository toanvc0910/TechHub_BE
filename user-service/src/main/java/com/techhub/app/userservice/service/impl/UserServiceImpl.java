package com.techhub.app.userservice.service.impl;

import com.techhub.app.userservice.dto.request.ChangePasswordRequest;
import com.techhub.app.userservice.dto.request.CreateUserRequest;
import com.techhub.app.userservice.dto.request.ForgotPasswordRequest;
import com.techhub.app.userservice.dto.request.ResetPasswordRequest;
import com.techhub.app.userservice.dto.request.UpdateUserRequest;
import com.techhub.app.userservice.dto.response.UserResponse;
import com.techhub.app.userservice.entity.Role;
import com.techhub.app.userservice.entity.User;
import com.techhub.app.userservice.entity.UserRole;
import com.techhub.app.userservice.enums.UserStatus;
import com.techhub.app.userservice.enums.UserRoleEnum;
import com.techhub.app.userservice.repository.RoleRepository;
import com.techhub.app.userservice.repository.UserRepository;
import com.techhub.app.userservice.repository.UserRoleRepository;
import com.techhub.app.userservice.service.EmailService;
import com.techhub.app.userservice.service.UserService;
import com.techhub.app.userservice.service.OTPService;
import com.techhub.app.userservice.enums.OTPTypeEnum;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final UserRoleRepository userRoleRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;
    private final OTPService otpService;

    @Override
    @Transactional
    public UserResponse createUser(CreateUserRequest request) {
        log.info("Creating user with email: {}", request.getEmail());

        if (userRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("Email already exists");
        }

        if (request.getUsername() != null && userRepository.existsByUsername(request.getUsername())) {
            throw new RuntimeException("Username already exists");
        }

        User user = new User();
        user.setEmail(request.getEmail());
        user.setUsername(request.getUsername());
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setRole(UserRoleEnum.LEARNER);  // Set ENUM role to satisfy NOT NULL in DB
        user.setStatus(UserStatus.ACTIVE);
        user.setIsActive(true);
        user.setCreated(LocalDateTime.now());
        user.setUpdated(LocalDateTime.now());

        user = userRepository.save(user);  // Save user first

        log.info("User created successfully with ID: {}", user.getId());

        // Assign default LEARNER role via join table (many-to-many)
        Role learnerRoleEntity = roleRepository.findByName("LEARNER")
                .orElseThrow(() -> new RuntimeException("Default LEARNER role not found"));

        UserRole userRoleEntity = new UserRole();
        // Manually set IDs for composite key (assuming UserRole uses @EmbeddedId or separate userId/roleId fields)
        userRoleEntity.setUserId(user.getId());  // ← Fix: Explicitly set user_id
        userRoleEntity.setRoleId(learnerRoleEntity.getId());  // ← Fix: Explicitly set role_id
        userRoleEntity.setIsActive(true);
        userRoleEntity.setAssignedAt(LocalDateTime.now());
        userRoleEntity.setCreated(LocalDateTime.now());
        userRoleEntity.setUpdated(LocalDateTime.now());
        userRoleRepository.save(userRoleEntity);  // Now IDs are set, save succeeds

        // Send welcome email
        try {
            emailService.sendWelcomeEmail(user.getEmail(), user.getUsername());
            log.info("Welcome email sent to: {}", user.getEmail());
        } catch (Exception e) {
            log.error("Failed to send welcome email to: {}", user.getEmail(), e);
            // Don't fail registration if email fails
        }

        return convertToUserResponse(user);
    }

    @Override
    public UserResponse getUserById(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found with ID: " + userId));
        return convertToUserResponse(user);
    }

    @Override
    public UserResponse getUserByEmail(String email) {
        User user = userRepository.findByEmailAndIsActiveTrue(email)
                .orElseThrow(() -> new RuntimeException("User not found with email: " + email));
        return convertToUserResponse(user);
    }

    @Override
    public UserResponse getUserByUsername(String username) {
        User user = userRepository.findByUsernameAndIsActiveTrue(username)
                .orElseThrow(() -> new RuntimeException("User not found with username: " + username));
        return convertToUserResponse(user);
    }

    @Override
    @Transactional
    public UserResponse updateUser(UUID userId, UpdateUserRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found with ID: " + userId));

        if (request.getUsername() != null && !request.getUsername().equals(user.getUsername())) {
            if (userRepository.existsByUsername(request.getUsername())) {
                throw new RuntimeException("Username already exists");
            }
            user.setUsername(request.getUsername());
        }

        user.setUpdated(LocalDateTime.now());
        User savedUser = userRepository.save(user);
        return convertToUserResponse(savedUser);
    }

    @Override
    @Transactional
    public void deleteUser(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found with ID: " + userId));
        user.setIsActive(false);
        user.setUpdated(LocalDateTime.now());
        userRepository.save(user);
        log.info("User soft deleted: {}", userId);
    }

    @Override
    @Transactional
    public void changePassword(UUID userId, ChangePasswordRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found with ID: " + userId));

        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPasswordHash())) {
            throw new RuntimeException("Current password is incorrect");
        }

        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            throw new RuntimeException("Password confirmation does not match");
        }

        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        user.setUpdated(LocalDateTime.now());
        userRepository.save(user);

        log.info("Password changed for user: {}", userId);
    }

    @Override
    public void forgotPassword(ForgotPasswordRequest request) {
        User user = userRepository.findByEmailAndIsActiveTrue(request.getEmail())
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Generate OTP and send email
        String otpCode = otpService.generateOTP();
        OTPTypeEnum otpType = OTPTypeEnum.RESET;

        try {
            // Save OTP to database using userId
            otpService.saveOTP(user.getId(), otpCode, otpType);

            // Send password reset email with OTP
            emailService.sendPasswordResetEmail(user.getEmail(), otpCode);

            log.info("Password reset OTP sent to: {}", user.getEmail());
        } catch (Exception e) {
            log.error("Failed to process forgot password for: {}", user.getEmail(), e);
            throw new RuntimeException("Failed to send password reset email");
        }
    }

    @Override
    @Transactional
    public void resetPassword(String email, ResetPasswordRequest request) {
        User user = userRepository.findByEmailAndIsActiveTrue(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Validate OTP using userId
        if (!otpService.validateOTP(user.getId(), request.getOtp(), OTPTypeEnum.RESET)) {
            throw new RuntimeException("Invalid or expired OTP code");
        }

        // Validate password confirmation
        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            throw new RuntimeException("Password confirmation does not match");
        }

        // Update password
        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        user.setUpdated(LocalDateTime.now());
        userRepository.save(user);

        // Clean up used OTP
        otpService.deleteOTP(user.getId(), OTPTypeEnum.RESET);

        log.info("Password reset successfully for user: {}", email);
    }

    @Override
    @Transactional
    public void activateUser(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found with ID: " + userId));
        user.setStatus(UserStatus.ACTIVE);
        user.setIsActive(true);
        user.setUpdated(LocalDateTime.now());
        userRepository.save(user);
    }

    @Override
    @Transactional
    public void deactivateUser(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found with ID: " + userId));
        user.setStatus(UserStatus.INACTIVE);
        user.setUpdated(LocalDateTime.now());
        userRepository.save(user);
    }

    @Override
    @Transactional
    public void changeUserStatus(UUID userId, UserStatus status) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found with ID: " + userId));
        user.setStatus(status);
        user.setUpdated(LocalDateTime.now());
        userRepository.save(user);
    }

    @Override
    public Page<UserResponse> getAllUsers(Pageable pageable) {
        Page<User> users = userRepository.findByIsActiveTrueOrderByCreatedDesc(pageable);
        return users.map(this::convertToUserResponse);
    }

    @Override
    public Page<UserResponse> getUsersByStatus(UserStatus status, Pageable pageable) {
        Page<User> users = userRepository.findByStatusAndIsActiveTrueOrderByCreatedDesc(status, pageable);
        return users.map(this::convertToUserResponse);
    }

    @Override
    public Page<UserResponse> searchUsers(String keyword, Pageable pageable) {
        Page<User> users = userRepository.searchUsers(keyword, pageable);
        return users.map(this::convertToUserResponse);
    }

    @Override
    public boolean existsByEmail(String email) {
        return userRepository.existsByEmail(email);
    }

    @Override
    public boolean existsByUsername(String username) {
        return userRepository.existsByUsername(username);
    }

    @Override
    public long countUsersByStatus(UserStatus status) {
        return userRepository.countByStatusAndIsActiveTrue(status);
    }

    private UserResponse convertToUserResponse(User user) {
        List<String> roles;
        try {
            // Try to get roles from user.getUserRoles() first
            roles = user.getUserRoles().stream()
                    .filter(UserRole::getIsActive)
                    .map(userRole -> userRole.getRole().getName())
                    .collect(Collectors.toList());

            log.debug("Loaded {} roles for user {}: {}", roles.size(), user.getEmail(), roles);

            // If no roles found, add default role based on user.role enum
            if (roles.isEmpty()) {
                roles.add(user.getRole().name());
                log.warn("No UserRole entities found for user {}, using default role: {}",
                        user.getEmail(), user.getRole().name());
            }
        } catch (Exception e) {
            log.error("Error loading roles for user {}, falling back to default role: {}",
                    user.getEmail(), e.getMessage());
            // Fallback to the role enum field
            roles = List.of(user.getRole().name());
        }

        return UserResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .username(user.getUsername())
                .roles(roles)
                .status(user.getStatus().name())
                .created(user.getCreated())
                .updated(user.getUpdated())
                .isActive(user.getIsActive())
                .build();
    }
}

