package com.techhub.app.userservice.oauth2;

import com.techhub.app.userservice.entity.User;
import com.techhub.app.userservice.entity.AuthProvider;
import com.techhub.app.userservice.enums.AuthProviderEnum;
import com.techhub.app.userservice.repository.UserRepository;
import com.techhub.app.userservice.repository.AuthProviderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Optional;

@Component
@RequiredArgsConstructor
@Slf4j
public class OAuth2AuthenticationSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final UserRepository userRepository;
    private final AuthProviderRepository authProviderRepository;

    @Value("${app.oauth2.authorized-redirect-uris:http://localhost:3000/oauth2/redirect}")
    private String redirectUri;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                      Authentication authentication) throws IOException, ServletException {

        String targetUrl = determineTargetUrl(request, response, authentication);

        if (response.isCommitted()) {
            logger.debug("Response has already been committed. Unable to redirect to " + targetUrl);
            return;
        }

        clearAuthenticationAttributes(request);
        getRedirectStrategy().sendRedirect(request, response, targetUrl);
    }

    protected String determineTargetUrl(HttpServletRequest request, HttpServletResponse response,
                                      Authentication authentication) {

        UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();

        // Find user by email to get complete user info
        Optional<User> userOptional = userRepository.findByEmail(userPrincipal.getEmail());
        if (userOptional.isEmpty()) {
            log.error("User not found after OAuth2 authentication: {}", userPrincipal.getEmail());
            return redirectUri + "?error=user_not_found";
        }

        User user = userOptional.get();

        try {
            // Detect provider from current authentication token, fallback to existing provider mapping
            String providerName = "unknown";
            if (authentication instanceof OAuth2AuthenticationToken) {
                providerName = ((OAuth2AuthenticationToken) authentication)
                        .getAuthorizedClientRegistrationId()
                        .toLowerCase();
            } else {
                for (AuthProviderEnum provider : AuthProviderEnum.values()) {
                    if (authProviderRepository.existsByUserIdAndProvider(user.getId(), provider)) {
                        providerName = provider.name().toLowerCase();
                        break;
                    }
                }
            }

            log.info("OAuth2 login successful for user: {} via provider: {}",
                    user.getEmail(), providerName);

            // Redirect to proxy-client for JWT token generation
            // Instead of generating JWT here, pass user info to proxy-client
            return UriComponentsBuilder.fromUriString(redirectUri)
                    .queryParam("userId", user.getId())
                    .queryParam("email", user.getEmail())
                    .queryParam("username", user.getUsername())
                    .queryParam("provider", providerName)
                    .queryParam("oauth2Success", "true")
                    .build().toUriString();

        } catch (Exception e) {
            log.error("Error processing OAuth2 user: {}", user.getEmail(), e);
            return redirectUri + "?error=processing_failed";
        }
    }
}
