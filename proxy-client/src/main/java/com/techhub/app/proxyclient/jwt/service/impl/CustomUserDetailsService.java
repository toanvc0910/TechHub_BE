package com.techhub.app.proxyclient.jwt.service.impl;

import com.techhub.app.proxyclient.jwt.domain.AuthUserDetails;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Collections;

@Slf4j
@Service
public class CustomUserDetailsService implements com.techhub.app.proxyclient.jwt.service.CustomUserDetailsService {

    @Override
    public UserDetails loadUserByUsernameAndTenantId(String username, Integer tenantId) throws UsernameNotFoundException {
        if (username == null || username.isBlank()) {
            throw new UsernameNotFoundException("Username is blank");
        }
        return User.builder()
                .username(username)
                // BCrypt hash for "password"
                .password("$2a$10$7EqJtq98hPqEX7fNZaFWoOhi5g5G7xO6jwxKF1u9Ii.YivGi99ZGa")
                .authorities(Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER")))
                .build();
    }

    @Override
    public UserDetails loadUserByUsernameAndTenantIdInternal(String username, String password,
                                                             Integer tenantId, Integer userId) {
        return User.builder()
                .username(username)
                .password(password)
                .authorities(Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER")))
                .build();
    }

    @Override
    public AuthUserDetails loadByClientIdAndClientSecretAndGrantType(String clientId, String clientSecret, String grantType) {
        if (clientId == null || clientSecret == null || grantType == null) {
            throw new UsernameNotFoundException("Invalid client credentials");
        }
        return new AuthUserDetails(clientId, clientSecret, grantType);
    }

    @Override
    public AuthUserDetails loadByClientId(String clientId) {
        if (clientId == null || clientId.isBlank()) return null;
        return new AuthUserDetails(clientId, "", "client_credentials");
    }

    @Override
    public UserDetails loadUserByUsername(String userName) throws UsernameNotFoundException {
        return loadUserByUsernameAndTenantId(userName, 0);
    }
}