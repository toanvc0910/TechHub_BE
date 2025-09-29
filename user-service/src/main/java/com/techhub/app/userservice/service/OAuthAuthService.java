package com.techhub.app.userservice.service;

import com.techhub.app.commonservice.jwt.JwtUtil;
import com.techhub.app.userservice.dto.response.AuthResponse;
import com.techhub.app.userservice.entity.AuthProvider;
import com.techhub.app.userservice.entity.Role;
import com.techhub.app.userservice.entity.User;
import com.techhub.app.userservice.entity.UserRole;
import com.techhub.app.userservice.enums.AuthProviderType;
import com.techhub.app.userservice.enums.UserRoleEnum;
import com.techhub.app.userservice.enums.UserStatus;
import com.techhub.app.userservice.repository.AuthProviderRepository;
import com.techhub.app.userservice.repository.RoleRepository;
import com.techhub.app.userservice.repository.UserRepository;
import com.techhub.app.userservice.repository.UserRoleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class OAuthAuthService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final UserRoleRepository userRoleRepository;
    private final AuthProviderRepository authProviderRepository;
    private final JwtUtil jwtUtil;
    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${oauth.google.client-id:}")
    private String googleClientId;

    @Value("${oauth.facebook.app-id:}")
    private String facebookAppId;

    @Value("${oauth.facebook.app-secret:}")
    private String facebookAppSecret;

    @Transactional
    public AuthResponse loginWithGoogle(String idToken, String accessToken) {
        String email;
        String name;

        if (idToken != null && !idToken.isBlank()) {
            // Verify ID token via Google endpoint
            String url = "https://oauth2.googleapis.com/tokeninfo?id_token=" + idToken;
            Map<?, ?> info = restTemplate.getForObject(url, Map.class);
            if (info == null || info.get("aud") == null) throw new RuntimeException("Invalid Google token");
            if (googleClientId != null && !googleClientId.isBlank()) {
                if (!googleClientId.equals(info.get("aud"))) throw new RuntimeException("Google client mismatch");
            }
            email = (String) info.get("email");
            name = (String) info.get("name");
            if (email == null || email.isBlank()) throw new RuntimeException("Google email not available");
        } else if (accessToken != null && !accessToken.isBlank()) {
            // Fallback: use access token to fetch userinfo
            String url = "https://www.googleapis.com/oauth2/v1/userinfo?alt=json&access_token=" + accessToken;
            Map<?, ?> info = restTemplate.getForObject(url, Map.class);
            if (info == null) throw new RuntimeException("Invalid Google access token");
            email = (String) info.get("email");
            name = (String) info.get("name");
            if (email == null || email.isBlank()) throw new RuntimeException("Google email not available");
        } else {
            throw new RuntimeException("Missing Google token");
        }

        return loginOrCreate(email, name, AuthProviderType.GOOGLE, accessToken);
    }

    @Transactional
    public AuthResponse loginWithFacebook(String accessToken) {
        if (accessToken == null || accessToken.isBlank()) throw new RuntimeException("Missing Facebook access token");

        // Validate token via debug endpoint
        if (facebookAppId == null || facebookAppId.isBlank() || facebookAppSecret == null || facebookAppSecret.isBlank()) {
            throw new RuntimeException("Facebook App credentials not configured");
        }
        String appToken = facebookAppId + "|" + facebookAppSecret;
        String debugUrl = "https://graph.facebook.com/debug_token?input_token=" + accessToken + "&access_token=" + appToken;
        Map<?, ?> debug = restTemplate.getForObject(debugUrl, Map.class);
        if (debug == null || debug.get("data") == null || !Boolean.TRUE.equals(((Map<?, ?>) debug.get("data")).get("is_valid"))) {
            throw new RuntimeException("Invalid Facebook token");
        }

        // Fetch profile
        String url = "https://graph.facebook.com/me?fields=id,name,email&access_token=" + accessToken;
        Map<?, ?> profile = restTemplate.getForObject(url, Map.class);
        if (profile == null) throw new RuntimeException("Unable to fetch Facebook profile");
        String email = (String) profile.get("email");
        String name = (String) profile.get("name");
        if (email == null || email.isBlank()) throw new RuntimeException("Facebook email not available (request email scope)");

        return loginOrCreate(email, name, AuthProviderType.FACEBOOK, accessToken);
    }

    @Transactional
    public AuthResponse loginWithGithub(String accessToken) {
        if (accessToken == null || accessToken.isBlank()) throw new RuntimeException("Missing GitHub access token");

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "token " + accessToken);
        headers.set("Accept", "application/vnd.github+json");

        ResponseEntity<Map> userResp = restTemplate.exchange(
                "https://api.github.com/user", HttpMethod.GET, new HttpEntity<>(headers), Map.class);
        Map<?, ?> user = userResp.getBody();
        if (user == null) throw new RuntimeException("Invalid GitHub token");

        String email = (String) user.get("email");
        String name = (String) user.get("name");
        if (email == null || email.isBlank()) {
            // Need to query emails endpoint
            ResponseEntity<List> emailsResp = restTemplate.exchange(
                    "https://api.github.com/user/emails", HttpMethod.GET, new HttpEntity<>(headers), List.class);
            List<?> emails = emailsResp.getBody();
            if (emails != null) {
                Optional<?> primary = emails.stream().filter(e -> Boolean.TRUE.equals(((Map<?, ?>) e).get("primary"))).findFirst();
                if (primary.isPresent()) email = (String) ((Map<?, ?>) primary.get()).get("email");
            }
        }
        if (email == null || email.isBlank()) throw new RuntimeException("GitHub email not available (request user:email scope)");

        return loginOrCreate(email, name, AuthProviderType.GITHUB, accessToken);
    }

    private AuthResponse loginOrCreate(String email, String name, AuthProviderType providerType, String accessToken) {
        User user = userRepository.findByEmailAndIsActiveTrue(email).orElse(null);
        
        // Check if user exists but is inactive
        if (user == null) {
            Optional<User> inactiveUser = userRepository.findByEmail(email);
            if (inactiveUser.isPresent() && !inactiveUser.get().getIsActive()) {
                throw new RuntimeException("Account is disabled. Please contact support.");
            }
        }
        
        if (user == null) {
            user = new User();
            user.setEmail(email);
            user.setUsername(name != null ? name : email);
            user.setPasswordHash(null);
            user.setRole(UserRoleEnum.LEARNER);
            user.setStatus(UserStatus.ACTIVE);
            user.setIsActive(true);
            user.setCreated(LocalDateTime.now());
            user.setUpdated(LocalDateTime.now());
            user = userRepository.save(user);

            Role learnerRoleEntity = roleRepository.findByName("LEARNER")
                    .orElseThrow(() -> new RuntimeException("Default LEARNER role not found"));

            UserRole userRole = new UserRole();
            userRole.setUserId(user.getId());
            userRole.setRoleId(learnerRoleEntity.getId());
            userRole.setIsActive(true);
            userRoleRepository.save(userRole);
        }

        List<String> roles = userRoleRepository.findByUserId(user.getId()).stream()
                .map(ur -> ur.getRole().getName())
                .collect(Collectors.toList());

        // Save or update AuthProvider information
        AuthProvider authProvider = authProviderRepository
                .findByUserIdAndProviderAndIsActiveTrue(user.getId(), providerType)
                .orElse(new AuthProvider());
        
        authProvider.setUser(user);
        authProvider.setProvider(providerType);
        authProvider.setAccessToken(accessToken);
        authProvider.setIsActive(true);
        authProvider.setCreated(authProvider.getCreated() != null ? authProvider.getCreated() : LocalDateTime.now());
        authProvider.setUpdated(LocalDateTime.now());
        authProviderRepository.save(authProvider);

        String jwtAccessToken = jwtUtil.generateToken(user.getId(), user.getEmail(), roles);
        String refreshToken = jwtUtil.generateRefreshToken(user.getId(), user.getEmail());

        AuthResponse.UserInfo userInfo = AuthResponse.UserInfo.builder()
                .id(user.getId())
                .email(user.getEmail())
                .username(user.getUsername())
                .roles(roles)
                .status(user.getStatus().name())
                .build();

        return AuthResponse.builder()
                .accessToken(jwtAccessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .expiresIn(86400)
                .user(userInfo)
                .build();
    }
}
