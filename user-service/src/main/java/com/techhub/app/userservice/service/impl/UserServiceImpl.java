package com.techhub.app.userservice.service.impl;

import com.techhub.app.commonservice.jwt.JwtUtil;
import com.techhub.app.userservice.dto.request.ChangePasswordRequest;
import com.techhub.app.userservice.dto.request.CreateUserRequest;
import com.techhub.app.userservice.dto.request.ForgotPasswordRequest;
import com.techhub.app.userservice.dto.request.ResetPasswordRequest;
import com.techhub.app.userservice.dto.request.UpdateUserRequest;
import com.techhub.app.userservice.dto.response.UserResponse;
import com.techhub.app.userservice.entity.Role;
import com.techhub.app.userservice.entity.User;
import com.techhub.app.userservice.entity.UserRole;
import com.techhub.app.userservice.entity.UserRoleId;
import com.techhub.app.userservice.enums.UserStatus;
import com.techhub.app.userservice.enums.UserRoleEnum;  // Import enum UserRole (LEARNER, INSTRUCTOR, ADMIN)
import com.techhub.app.userservice.repository.RoleRepository;
import com.techhub.app.userservice.repository.UserRepository;
import com.techhub.app.userservice.repository.UserRoleRepository;
import com.techhub.app.userservice.service.EmailService;
import com.techhub.app.userservice.service.UserService;
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
    private final JwtUtil jwtUtil;

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

        // Generate OTP and send email (implementation depends on your OTP service)
        // For now, just log it
        log.info("Password reset requested for user: {}", request.getEmail());

        // In a real implementation, you would:
        // 1. Generate OTP
        // 2. Save OTP to database with expiration
        // 3. Send email with OTP
    }

    @Override
    @Transactional
    public void resetPassword(String email, ResetPasswordRequest request) {
        User user = userRepository.findByEmailAndIsActiveTrue(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Validate OTP (implementation depends on your OTP service)
        // For now, just validate password confirmation
        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            throw new RuntimeException("Password confirmation does not match");
        }

        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        user.setUpdated(LocalDateTime.now());
        userRepository.save(user);

        log.info("Password reset for user: {}", email);
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

    @Override
    public User getCurrentUser(String token) {
        UUID userId = jwtUtil.getUserIdFromToken(token);
        return userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found with ID: " + userId));
    }

    private UserResponse convertToUserResponse(User user) {
        List<String> roles = user.getUserRoles().stream()
                .map(userRole -> userRole.getRole().getName())
                .collect(Collectors.toList());

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