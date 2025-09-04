package com.techhub.app.proxyclient.jwt.domain;

import lombok.RequiredArgsConstructor;
import org.common.dbiz.dto.userDto.AuthDto;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;

@RequiredArgsConstructor
public class AuthUserDetails implements UserDetails {

    private final String clientId;
    private final String clientSecret;
    private final String grantType;
    private Collection<? extends GrantedAuthority> authorities;

    public AuthUserDetails(AuthDto dto, Collection<? extends GrantedAuthority> authorities) {
        this.clientId = dto.getClientId();
        this.clientSecret = dto.getClientSecret();
        this.grantType = dto.getGrantType();
        this.authorities = authorities;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return this.authorities;
    }

    @Override
    public String getPassword() {
        return this.clientSecret;
    }

    @Override
    public String getUsername() {
        return this.clientId;
    }

    @Override
    public boolean isAccountNonExpired() {
        return false;
    }

    @Override
    public boolean isAccountNonLocked() {
        return false;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return false;
    }

    @Override
    public boolean isEnabled() {
        return false;
    }
}
