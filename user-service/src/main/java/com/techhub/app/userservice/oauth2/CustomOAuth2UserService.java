package com.techhub.app.userservice.oauth2;

import com.techhub.app.userservice.dto.oauth.OAuth2UserInfo;
import com.techhub.app.userservice.entity.User;
import com.techhub.app.userservice.entity.Role;
import com.techhub.app.userservice.entity.UserRole;
import com.techhub.app.userservice.entity.AuthProvider;
import com.techhub.app.userservice.enums.UserStatus;
import com.techhub.app.userservice.enums.UserRoleEnum;
import com.techhub.app.userservice.enums.AuthProviderEnum;
import com.techhub.app.userservice.repository.UserRepository;
import com.techhub.app.userservice.repository.RoleRepository;
import com.techhub.app.userservice.repository.UserRoleRepository;
import com.techhub.app.userservice.repository.AuthProviderRepository;
import com.techhub.app.userservice.service.EmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.InternalAuthenticationServiceException;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final UserRoleRepository userRoleRepository;
    private final AuthProviderRepository authProviderRepository;
    private final EmailService emailService;

    @Override
    public OAuth2User loadUser(OAuth2UserRequest oAuth2UserRequest) throws OAuth2AuthenticationException {
        OAuth2User oAuth2User = super.loadUser(oAuth2UserRequest);

        try {
            return processOAuth2User(oAuth2UserRequest, oAuth2User);
        } catch (Exception ex) {
            log.error("Error processing OAuth2 user", ex);
            throw new InternalAuthenticationServiceException(ex.getMessage(), ex.getCause());
        }
    }

    @Transactional
    private OAuth2User processOAuth2User(OAuth2UserRequest oAuth2UserRequest, OAuth2User oAuth2User) {
        OAuth2UserInfo oAuth2UserInfo = OAuth2UserInfoFactory.getOAuth2UserInfo(
                oAuth2UserRequest.getClientRegistration().getRegistrationId(),
                oAuth2User.getAttributes()
        );

        if (!StringUtils.hasText(oAuth2UserInfo.getEmail())) {
            throw new OAuth2AuthenticationException("Email not found from OAuth2 provider");
        }

        Optional<User> userOptional = userRepository.findByEmail(oAuth2UserInfo.getEmail());
        User user;

        if (userOptional.isPresent()) {
            user = userOptional.get();
            user = updateExistingUser(user, oAuth2UserRequest, oAuth2UserInfo);
        } else {
            user = registerNewUser(oAuth2UserRequest, oAuth2UserInfo);
        }

        return UserPrincipal.create(user, oAuth2User.getAttributes());
    }

    private User registerNewUser(OAuth2UserRequest oAuth2UserRequest, OAuth2UserInfo oAuth2UserInfo) {
        User user = new User();
        user.setEmail(oAuth2UserInfo.getEmail());
        user.setUsername(generateUsernameFromEmail(oAuth2UserInfo.getEmail()));
        user.setRole(UserRoleEnum.LEARNER);
        user.setStatus(UserStatus.ACTIVE);
        user.setIsActive(true);
        user.setCreated(LocalDateTime.now());
        user.setUpdated(LocalDateTime.now());

        user = userRepository.save(user);

        // Assign default LEARNER role
        Role learnerRole = roleRepository.findByName("LEARNER")
                .orElseThrow(() -> new RuntimeException("Default LEARNER role not found"));

        UserRole userRole = new UserRole();
        userRole.setUserId(user.getId());
        userRole.setRoleId(learnerRole.getId());
        userRole.setIsActive(true);
        userRole.setAssignedAt(LocalDateTime.now());
        userRole.setCreated(LocalDateTime.now());
        userRole.setUpdated(LocalDateTime.now());
        userRoleRepository.save(userRole);

        // Create AuthProvider entry
        createAuthProvider(user.getId(), oAuth2UserRequest, oAuth2UserInfo);

        // Send welcome email
        try {
            emailService.sendWelcomeEmail(user.getEmail(), user.getUsername());
            log.info("Welcome email sent to OAuth2 user: {}", user.getEmail());
        } catch (Exception e) {
            log.error("Failed to send welcome email to OAuth2 user: {}", user.getEmail(), e);
        }

        log.info("New OAuth2 user registered: {} via {}", user.getEmail(), oAuth2UserRequest.getClientRegistration().getRegistrationId());
        return user;
    }

    private User updateExistingUser(User existingUser, OAuth2UserRequest oAuth2UserRequest, OAuth2UserInfo oAuth2UserInfo) {
        existingUser.setUpdated(LocalDateTime.now());
        User savedUser = userRepository.save(existingUser);

        // Update or create AuthProvider entry
        AuthProviderEnum provider = AuthProviderEnum.valueOf(oAuth2UserRequest.getClientRegistration().getRegistrationId().toUpperCase());
        Optional<AuthProvider> authProviderOpt = authProviderRepository.findByUserIdAndProvider(savedUser.getId(), provider);

        if (authProviderOpt.isEmpty()) {
            createAuthProvider(savedUser.getId(), oAuth2UserRequest, oAuth2UserInfo);
        }

        return savedUser;
    }

    private void createAuthProvider(UUID userId, OAuth2UserRequest oAuth2UserRequest, OAuth2UserInfo oAuth2UserInfo) {
        AuthProvider authProvider = new AuthProvider();
        authProvider.setUserId(userId);
        authProvider.setProvider(AuthProviderEnum.valueOf(oAuth2UserRequest.getClientRegistration().getRegistrationId().toUpperCase()));
        authProvider.setAccessToken(oAuth2UserRequest.getAccessToken().getTokenValue());
        authProvider.setCreated(LocalDateTime.now());
        authProvider.setUpdated(LocalDateTime.now());
        authProvider.setIsActive(true);

        authProviderRepository.save(authProvider);
    }

    private String generateUsernameFromEmail(String email) {
        String username = email.substring(0, email.indexOf("@"));

        // Check if username already exists, if yes, append random number
        if (userRepository.existsByUsername(username)) {
            username = username + "_" + System.currentTimeMillis();
        }

        return username;
    }
}
