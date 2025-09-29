package com.techhub.app.userservice.dto.request;

import lombok.Data;

@Data
public class OAuthLoginRequest {
    // For Google: idToken or accessToken (prefer idToken)
    private String idToken;
    private String accessToken;
}

