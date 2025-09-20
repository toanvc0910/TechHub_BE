package com.techhub.app.proxyclient.jwt.domain;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class AuthUserDetails {
    private final String clientId;
    private final String clientSecret;
    private final String grantType;

    public String getUsername() {
        return clientId;
    }
}
