package com.techhub.app.userservice.service.impl;

import com.techhub.app.commonservice.exception.BadRequestException;
import com.techhub.app.commonservice.exception.ConflictException;
import com.techhub.app.commonservice.exception.ForbiddenException;
import com.techhub.app.commonservice.exception.NotFoundException;
import com.techhub.app.commonservice.exception.UnauthorizedException;
import com.techhub.app.userservice.dto.request.ChangePasswordRequest;
import com.techhub.app.userservice.dto.request.CreateUserRequest;
import com.techhub.app.userservice.dto.request.ForgotPasswordRequest;
import com.techhub.app.userservice.dto.request.ResetPasswordRequest;
import com.techhub.app.userservice.dto.request.UpdateUserRequest;
import com.techhub.app.userservice.dto.request.VerifyEmailRequest;
import com.techhub.app.userservice.dto.response.UserResponse;
import com.techhub.app.userservice.entity.Role;
import com.techhub.app.userservice.entity.User;
import com.techhub.app.userservice.entity.UserRole;
import com.techhub.app.userservice.enums.OTPTypeEnum;
import com.techhub.app.userservice.enums.UserStatus;
import com.techhub.app.userservice.repository.RoleRepository;
import com.techhub.app.userservice.repository.UserRepository;
import com.techhub.app.userservice.repository.UserRoleRepository;
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

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserServiceImpl implements UserService {

    private static final String DEFAULT_ROLE_NAME = "LEARNER";

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final UserRoleRepository userRoleRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;
    private final OTPService otpService;

    @Override
    @Transactional
    public UserResponse registerUser(CreateUserRequest request) {
        User user = prepareUserForCreation(request, UserStatus.INACTIVE);
        boolean reactivated = user.getId() != null && userRepository.existsById(user.getId());

        user = userRepository.save(user);
        assignDefaultRole(user);

        String otp = otpService.generateOTP();
        otpService.saveOTP(user.getId(), otp, OTPTypeEnum.REGISTER);
        emailService.sendOTPEmail(user.getEmail(), otp, OTPTypeEnum.REGISTER.name());

        log.info("User {} {} for registration", user.getEmail(), reactivated ? "reactivated" : "created");
        return convertToUserResponse(user);
    }

    @Override
    @Transactional
    public UserResponse verifyUserRegistration(VerifyEmailRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new NotFoundException("User not found with email: " + request.getEmail()));

        if (Boolean.FALSE.equals(user.getIsActive())) {
            throw new ForbiddenException("Account has been deactivated");
        }
        if (user.getStatus() == UserStatus.BANNED) {
            throw new ForbiddenException("Account has been banned");
        }
        if (user.getStatus() == UserStatus.ACTIVE) {
            throw new ConflictException("Account already verified");
        }

        boolean validOtp = otpService.validateOTP(user.getId(), request.getCode(), OTPTypeEnum.REGISTER);
        if (!validOtp) {
            throw new BadRequestException("Invalid or expired verification code");
        }

        user.setStatus(UserStatus.ACTIVE);
        user.setUpdated(LocalDateTime.now());
        User saved = userRepository.save(user);
        otpService.deleteOTP(user.getId(), OTPTypeEnum.REGISTER);

        emailService.sendAccountActivationEmail(saved.getEmail(), saved.getUsername());
        emailService.sendWelcomeEmail(saved.getEmail(), saved.getUsername());

        log.info("User {} successfully verified email", saved.getEmail());
        return convertToUserResponse(saved);
    }

    @Override
    @Transactional
    public UserResponse createUser(CreateUserRequest request) {
        User user = prepareUserForCreation(request, UserStatus.ACTIVE);
        boolean reactivated = user.getId() != null && userRepository.existsById(user.getId());

        user = userRepository.save(user);
        assignDefaultRole(user);

        emailService.sendWelcomeEmail(user.getEmail(), user.getUsername());
        log.info("User {} {} by administrator", user.getEmail(), reactivated ? "reactivated" : "created");
        return convertToUserResponse(user);
    }

    @Override
    public UserResponse getUserById(UUID userId) {
        User user = findActiveUserById(userId);
        return convertToUserResponse(user);
    }

    @Override
    public UserResponse getUserByEmail(String email) {
        return userRepository.findByEmailAndIsActiveTrue(email)
                .map(this::convertToUserResponse)
                .orElseThrow(() -> new NotFoundException("User not found with email: " + email));
    }

    @Override
    public UserResponse getUserByUsername(String username) {
        return userRepository.findByUsernameAndIsActiveTrue(username)
                .map(this::convertToUserResponse)
                .orElseThrow(() -> new NotFoundException("User not found with username: " + username));
    }

    @Override
    @Transactional
    public UserResponse updateUser(UUID userId, UpdateUserRequest request) {
        User user = findActiveUserById(userId);

        Optional.ofNullable(request.getUsername())
                .map(String::trim)
                .filter(username -> !username.equalsIgnoreCase(user.getUsername()))
                .ifPresent(username -> {
                    ensureUsernameAvailable(username, user.getId());
                    user.setUsername(username);
                });

        // Update avatar if provided
        Optional.ofNullable(request.getAvatar())
                .map(String::trim)
                .ifPresent(user::setAvatar);

        user.setUpdated(LocalDateTime.now());
        User saved = userRepository.save(user);
        log.info("User {} updated", saved.getId());
        return convertToUserResponse(saved);
    }

    @Override
    @Transactional
    public void deleteUser(UUID userId) {
        User user = findActiveUserById(userId);
        user.setIsActive(false);
        user.setStatus(UserStatus.INACTIVE);
        user.setUpdated(LocalDateTime.now());
        userRepository.save(user);

        List<UserRole> roles = userRoleRepository.findByUserId(userId);
        roles.forEach(role -> {
            role.setIsActive(false);
            role.setUpdated(LocalDateTime.now());
        });
        userRoleRepository.saveAll(roles);

        log.info("User {} soft deleted", userId);
    }

    @Override
    @Transactional
    public void changePassword(UUID userId, ChangePasswordRequest request) {
        User user = findActiveUserById(userId);

        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPasswordHash())) {
            throw new UnauthorizedException("Current password is incorrect");
        }
        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            throw new BadRequestException("Password confirmation does not match");
        }

        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        user.setUpdated(LocalDateTime.now());
        userRepository.save(user);
        log.info("Password changed for user {}", userId);
    }

    @Override
    @Transactional
    public void forgotPassword(ForgotPasswordRequest request) {
        User user = userRepository.findByEmailAndIsActiveTrue(request.getEmail())
                .orElseThrow(() -> new NotFoundException("User not found with email: " + request.getEmail()));

        String otp = otpService.generateOTP();
        otpService.saveOTP(user.getId(), otp, OTPTypeEnum.RESET);
        emailService.sendPasswordResetEmail(user.getEmail(), otp);
        log.info("Password reset OTP generated for {}", user.getEmail());
    }

    @Override
    @Transactional
    public void resetPassword(String email, ResetPasswordRequest request) {
        User user = userRepository.findByEmailAndIsActiveTrue(email)
                .orElseThrow(() -> new NotFoundException("User not found with email: " + email));

        if (!otpService.validateOTP(user.getId(), request.getOtp(), OTPTypeEnum.RESET)) {
            throw new BadRequestException("Invalid or expired OTP code");
        }
        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            throw new BadRequestException("Password confirmation does not match");
        }

        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        user.setUpdated(LocalDateTime.now());
        userRepository.save(user);
        otpService.deleteOTP(user.getId(), OTPTypeEnum.RESET);
        log.info("Password reset successfully for {}", email);
    }

    @Override
    @Transactional
    public void activateUser(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User not found with ID: " + userId));
        user.setStatus(UserStatus.ACTIVE);
        user.setIsActive(true);
        user.setUpdated(LocalDateTime.now());
        userRepository.save(user);
        log.info("User {} activated", userId);
    }

    @Override
    @Transactional
    public void deactivateUser(UUID userId) {
        User user = findActiveUserById(userId);
        user.setStatus(UserStatus.INACTIVE);
        user.setUpdated(LocalDateTime.now());
        userRepository.save(user);
        log.info("User {} deactivated", userId);
    }

    @Override
    @Transactional
    public void changeUserStatus(UUID userId, UserStatus status) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User not found with ID: " + userId));
        user.setStatus(status);
        user.setUpdated(LocalDateTime.now());
        userRepository.save(user);
        log.info("User {} status changed to {}", userId, status);
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
        return userRepository.existsByEmail(normalizeEmail(email));
    }

    @Override
    public boolean existsByUsername(String username) {
        return userRepository.existsByUsername(normalizeUsername(username));
    }

    @Override
    public long countUsersByStatus(UserStatus status) {
        return userRepository.countByStatusAndIsActiveTrue(status);
    }

    private User prepareUserForCreation(CreateUserRequest request, UserStatus status) {
        String email = normalizeEmail(request.getEmail());
        String username = normalizeUsername(request.getUsername());

        Optional<User> existingByEmail = userRepository.findByEmail(email);
        UUID existingId = existingByEmail.map(User::getId).orElse(null);

        ensureUsernameAvailable(username, existingId);

        if (existingByEmail.isPresent()) {
            User existing = existingByEmail.get();
            if (Boolean.TRUE.equals(existing.getIsActive())) {
                throw new ConflictException("Email already in use");
            }
            existing.setUsername(username);
            existing.setPasswordHash(passwordEncoder.encode(request.getPassword()));
            existing.setStatus(status);
            existing.setIsActive(true);
            existing.setUpdated(LocalDateTime.now());
            return existing;
        }

        LocalDateTime now = LocalDateTime.now();
        User user = new User();
        user.setEmail(email);
        user.setUsername(username);
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setStatus(status);
        user.setIsActive(true);
        user.setCreated(now);
        user.setUpdated(now);
        return user;
    }

    private void ensureUsernameAvailable(String username, UUID currentUserId) {
        userRepository.findByUsernameAndIsActiveTrue(username)
                .filter(existing -> currentUserId == null || !existing.getId().equals(currentUserId))
                .ifPresent(existing -> {
                    throw new ConflictException("Username already in use");
                });
    }

    private String normalizeEmail(String email) {
        if (email == null) {
            throw new BadRequestException("Email is required");
        }
        return email.trim().toLowerCase();
    }

    private String normalizeUsername(String username) {
        if (username == null) {
            throw new BadRequestException("Username is required");
        }
        return username.trim();
    }

    private void assignDefaultRole(User user) {
        Role defaultRole = roleRepository.findByName(DEFAULT_ROLE_NAME)
                .orElseThrow(() -> new NotFoundException("Default LEARNER role is not configured"));

        userRoleRepository.findByUserIdAndRoleId(user.getId(), defaultRole.getId())
                .ifPresentOrElse(existing -> {
                    if (Boolean.FALSE.equals(existing.getIsActive())) {
                        existing.setIsActive(true);
                        existing.setUpdated(LocalDateTime.now());
                        userRoleRepository.save(existing);
                    }
                }, () -> {
                    UserRole userRole = new UserRole();
                    userRole.setUserId(user.getId());
                    userRole.setRoleId(defaultRole.getId());
                    userRole.setUser(user);
                    userRole.setRole(defaultRole);
                    userRole.setAssignedAt(LocalDateTime.now());
                    userRole.setIsActive(Boolean.TRUE);
                    userRoleRepository.save(userRole);
                });
    }

    private User findActiveUserById(UUID userId) {
        return userRepository.findById(userId)
                .filter(User::getIsActive)
                .orElseThrow(() -> new NotFoundException("Active user not found with ID: " + userId));
    }

    private UserResponse convertToUserResponse(User user) {
        List<String> roles = user.getUserRoles().stream()
                .filter(UserRole::getIsActive)
                .map(userRole -> userRole.getRole().getName())
                .collect(Collectors.toList());

        return UserResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .username(user.getUsername())
                .avatar(user.getAvatar())
                .roles(roles)
                .status(user.getStatus().name())
                .created(user.getCreated())
                .updated(user.getUpdated())
                .isActive(user.getIsActive())
                .build();
    }
}
