package com.techhub.app.userservice.oauth2;

import com.techhub.app.userservice.dto.oauth.OAuth2UserInfo;
import com.techhub.app.userservice.entity.User;
import com.techhub.app.userservice.entity.Role;
import com.techhub.app.userservice.entity.UserRole;
import com.techhub.app.userservice.entity.AuthProvider;
import com.techhub.app.userservice.enums.UserStatus;
import com.techhub.app.userservice.enums.AuthProviderEnum;
import com.techhub.app.userservice.repository.UserRepository;
import com.techhub.app.userservice.repository.RoleRepository;
import com.techhub.app.userservice.repository.UserRoleRepository;
import com.techhub.app.userservice.repository.AuthProviderRepository;
import com.techhub.app.userservice.service.EmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.InternalAuthenticationServiceException;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
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
        String registrationId = oAuth2UserRequest.getClientRegistration().getRegistrationId();
        OAuth2UserInfo oAuth2UserInfo = OAuth2UserInfoFactory.getOAuth2UserInfo(
                registrationId,
                oAuth2User.getAttributes());

        if (!StringUtils.hasText(oAuth2UserInfo.getEmail())) {
            log.warn("Email not found in initial OAuth2 response from {}, attempting to resolve", registrationId);
            String resolvedEmail = resolveEmailFromProvider(oAuth2UserRequest);
            if (StringUtils.hasText(resolvedEmail)) {
                // Use setEmail method to update the mutable map
                if (oAuth2UserInfo instanceof com.techhub.app.userservice.dto.oauth.GitHubOAuth2UserInfo) {
                    ((com.techhub.app.userservice.dto.oauth.GitHubOAuth2UserInfo) oAuth2UserInfo)
                            .setEmail(resolvedEmail);
                } else if (oAuth2UserInfo instanceof com.techhub.app.userservice.dto.oauth.FacebookOAuth2UserInfo) {
                    ((com.techhub.app.userservice.dto.oauth.FacebookOAuth2UserInfo) oAuth2UserInfo)
                            .setEmail(resolvedEmail);
                } else if (oAuth2UserInfo instanceof com.techhub.app.userservice.dto.oauth.GoogleOAuth2UserInfo) {
                    ((com.techhub.app.userservice.dto.oauth.GoogleOAuth2UserInfo) oAuth2UserInfo)
                            .setEmail(resolvedEmail);
                }
                log.info("Successfully resolved email for {} user: {}", registrationId, resolvedEmail);
            } else {
                log.error("Failed to resolve email from {} provider. User cannot be authenticated.", registrationId);
                throw new OAuth2AuthenticationException(
                        "Email not found from " + registrationId
                                + " provider. Please ensure email permission is granted.");
            }
        }

        Optional<User> userOptional = userRepository.findByEmail(oAuth2UserInfo.getEmail());
        User user;

        if (userOptional.isPresent()) {
            user = userOptional.get();

            // Check if account is inactive - block login for ANY method (local or OAuth2)
            if (!user.getIsActive() || user.getStatus() == UserStatus.INACTIVE) {
                log.error("Attempted OAuth2 login with inactive account: {} via {}",
                        user.getEmail(), oAuth2UserRequest.getClientRegistration().getRegistrationId());
                throw new OAuth2AuthenticationException(
                        "Account has been deactivated. Please contact support.");
            }

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
        user.setStatus(UserStatus.ACTIVE);
        user.setLoginType(oAuth2UserRequest.getClientRegistration().getRegistrationId().toUpperCase());
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

        log.info("New OAuth2 user registered: {} via {}", user.getEmail(),
                oAuth2UserRequest.getClientRegistration().getRegistrationId());

        // Flush to ensure data is persisted immediately
        userRepository.flush();
        userRoleRepository.flush();

        // Re-fetch user with roles loaded to avoid LazyInitializationException
        return userRepository.findById(user.getId())
                .orElseThrow(() -> new RuntimeException("User not found after registration"));
    }

    private User updateExistingUser(User existingUser, OAuth2UserRequest oAuth2UserRequest,
            OAuth2UserInfo oAuth2UserInfo) {
        existingUser.setUpdated(LocalDateTime.now());
        existingUser.setLoginType(oAuth2UserRequest.getClientRegistration().getRegistrationId().toUpperCase());
        User savedUser = userRepository.save(existingUser);

        // Update or create AuthProvider entry
        AuthProviderEnum provider = AuthProviderEnum
                .valueOf(oAuth2UserRequest.getClientRegistration().getRegistrationId().toUpperCase());
        Optional<AuthProvider> authProviderOpt = authProviderRepository.findByUserIdAndProvider(savedUser.getId(),
                provider);

        if (authProviderOpt.isEmpty()) {
            createAuthProvider(savedUser.getId(), oAuth2UserRequest, oAuth2UserInfo);
        }

        // Flush to ensure data is persisted immediately
        userRepository.flush();

        // Re-fetch user with roles loaded to avoid LazyInitializationException
        return userRepository.findById(savedUser.getId())
                .orElseThrow(() -> new RuntimeException("User not found after update"));
    }

    private void createAuthProvider(UUID userId, OAuth2UserRequest oAuth2UserRequest, OAuth2UserInfo oAuth2UserInfo) {
        AuthProvider authProvider = new AuthProvider();
        authProvider.setUserId(userId);
        authProvider.setProvider(
                AuthProviderEnum.valueOf(oAuth2UserRequest.getClientRegistration().getRegistrationId().toUpperCase()));
        authProvider.setAccessToken(oAuth2UserRequest.getAccessToken().getTokenValue());
        authProvider.setCreated(LocalDateTime.now());
        authProvider.setUpdated(LocalDateTime.now());
        authProvider.setIsActive(true);

        authProviderRepository.save(authProvider);
    }

    private String generateUsernameFromEmail(String email) {
        // Username can be duplicated, only email must be unique
        return email.substring(0, email.indexOf("@"));
    }

    private String resolveEmailFromProvider(OAuth2UserRequest request) {
        String registrationId = request.getClientRegistration().getRegistrationId();

        try {
            RestTemplate restTemplate = new RestTemplate();
            HttpHeaders headers = new HttpHeaders();
            headers.setAccept(Arrays.asList(MediaType.APPLICATION_JSON));
            headers.setBearerAuth(request.getAccessToken().getTokenValue());
            HttpEntity<Void> entity = new HttpEntity<>(headers);

            if ("github".equalsIgnoreCase(registrationId)) {
                // GitHub requires separate API call to get emails
                var response = restTemplate.exchange(
                        "https://api.github.com/user/emails",
                        HttpMethod.GET,
                        entity,
                        new ParameterizedTypeReference<List<Map<String, Object>>>() {
                        });

                if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                    // Try to find primary verified email first
                    for (Map<String, Object> entry : response.getBody()) {
                        Object email = entry.get("email");
                        Object primary = entry.get("primary");
                        Object verified = entry.get("verified");
                        if (Boolean.TRUE.equals(primary) && Boolean.TRUE.equals(verified) && email != null) {
                            return email.toString();
                        }
                    }

                    // Fallback to any verified email
                    return response.getBody().stream()
                            .filter(map -> Boolean.TRUE.equals(map.get("verified")))
                            .map(map -> map.get("email"))
                            .filter(obj -> obj != null && StringUtils.hasText(obj.toString()))
                            .map(Object::toString)
                            .findFirst()
                            .orElse(null);
                }
            } else if ("facebook".equalsIgnoreCase(registrationId)) {
                // Facebook email should be in the initial user info if scope was granted
                // Try to fetch again with explicit email field request
                var response = restTemplate.exchange(
                        "https://graph.facebook.com/me?fields=email",
                        HttpMethod.GET,
                        entity,
                        new ParameterizedTypeReference<Map<String, Object>>() {
                        });

                if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                    Object email = response.getBody().get("email");
                    if (email != null && StringUtils.hasText(email.toString())) {
                        return email.toString();
                    }
                }
            }
        } catch (Exception ex) {
            log.error("Failed to resolve email for provider {}", registrationId, ex);
        }
        return null;
    }
}
