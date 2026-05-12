package com.techhub.app.userservice.service.impl;

import com.techhub.app.commonservice.exception.BadRequestException;
import com.techhub.app.userservice.dto.request.UpdateUserRequest;
import com.techhub.app.userservice.entity.User;
import com.techhub.app.userservice.enums.UserStatus;
import com.techhub.app.userservice.repository.RoleRepository;
import com.techhub.app.userservice.repository.UserRepository;
import com.techhub.app.userservice.repository.UserRoleRepository;
import com.techhub.app.userservice.service.EmailService;
import com.techhub.app.userservice.service.OTPService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private UserRoleRepository userRoleRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private EmailService emailService;

    @Mock
    private OTPService otpService;

    @InjectMocks
    private UserServiceImpl userService;

    @Test
    void updateUserWhenAdminPasswordChangeRequestedEncodesAndSavesPassword() {
        UUID userId = UUID.randomUUID();
        User user = activeUser(userId);
        UpdateUserRequest request = UpdateUserRequest.builder()
                .changePassword(true)
                .password("newpass1")
                .confirmPassword("newpass1")
                .build();

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(passwordEncoder.encode("newpass1")).thenReturn("encoded-newpass1");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        userService.updateUser(userId, request);

        assertEquals("encoded-newpass1", user.getPasswordHash());
        verify(userRepository).save(user);
        verify(emailService).sendPasswordChangedNotification(userId, user.getEmail(), user.getUsername());
    }

    @Test
    void updateUserWhenAdminPasswordConfirmationDoesNotMatchRejectsWithoutSaving() {
        UUID userId = UUID.randomUUID();
        User user = activeUser(userId);
        UpdateUserRequest request = UpdateUserRequest.builder()
                .changePassword(true)
                .password("newpass1")
                .confirmPassword("different")
                .build();

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        assertThrows(BadRequestException.class, () -> userService.updateUser(userId, request));

        assertEquals("old-password-hash", user.getPasswordHash());
        verify(passwordEncoder, never()).encode(any());
        verify(userRepository, never()).save(any());
        verify(emailService, never()).sendPasswordChangedNotification(any(), any(), any());
    }

    private User activeUser(UUID userId) {
        User user = new User();
        user.setId(userId);
        user.setEmail("learner@example.com");
        user.setUsername("learner");
        user.setPasswordHash("old-password-hash");
        user.setStatus(UserStatus.ACTIVE);
        user.setIsActive(true);
        user.setCreated(LocalDateTime.now().minusDays(1));
        user.setUpdated(LocalDateTime.now().minusDays(1));
        return user;
    }
}
