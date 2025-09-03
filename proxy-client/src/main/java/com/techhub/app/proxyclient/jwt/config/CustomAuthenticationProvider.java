package com.techhub.app.proxyclient.jwt.config;

import com.techhub.app.proxyclient.jwt.domain.CustomUsernamePasswordAuthenticationToken;
import com.techhub.app.proxyclient.jwt.service.impl.CustomUserDetailsService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CustomAuthenticationProvider implements AuthenticationProvider {
    private final CustomUserDetailsService userDetailsService;
    private final PasswordEncoder passwordEncoder;
    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        String username = authentication.getName();
        String password = authentication.getCredentials().toString();
        Integer clientId = ((CustomUsernamePasswordAuthenticationToken) authentication).getdTenantId();

        UserDetails userDetails = userDetailsService.loadUserByUsernameAndTenantId(username, clientId);
        if (userDetails == null || !passwordEncoder.matches(password, userDetails.getPassword())) {
            throw new BadCredentialsException("Bad credentials!");
        }

        return new CustomUsernamePasswordAuthenticationToken(username, password, clientId);

    }

    @Override
    public boolean supports(Class<?> authentication) {
        return CustomUsernamePasswordAuthenticationToken.class.isAssignableFrom(authentication);
    }
}
