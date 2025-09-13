package com.techhub.app.proxyclient.jwt.domain;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

public class CustomUsernamePasswordAuthenticationToken extends UsernamePasswordAuthenticationToken {
    private final Integer dTenantId;

    public CustomUsernamePasswordAuthenticationToken(Object principal, Object credentials, Integer dTenantId) {
        super(principal, credentials);
        this.dTenantId = dTenantId;
    }

    public Integer getdTenantId() {
        return dTenantId;
    }
}