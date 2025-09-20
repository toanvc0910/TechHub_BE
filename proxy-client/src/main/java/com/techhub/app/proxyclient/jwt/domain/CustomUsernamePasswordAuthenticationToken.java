package com.techhub.app.proxyclient.jwt.domain;

import lombok.Getter;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;

import java.util.Collection;

@Getter
public class CustomUsernamePasswordAuthenticationToken extends UsernamePasswordAuthenticationToken {
    private final Integer dTenantId;

    public CustomUsernamePasswordAuthenticationToken(Object principal, Object credentials, Integer dTenantId) {
        super(principal, credentials);
        this.dTenantId = dTenantId;
    }

    public CustomUsernamePasswordAuthenticationToken(Object principal, Object credentials, Integer dTenantId,
                                                     Collection<? extends GrantedAuthority> authorities) {
        super(principal, credentials, authorities);
        this.dTenantId = dTenantId;
    }

    public Integer getdTenantId() {
        return dTenantId;
    }
}
