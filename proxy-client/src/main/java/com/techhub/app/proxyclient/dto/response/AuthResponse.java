package com.techhub.app.proxyclient.dto.response;

import com.techhub.app.proxyclient.client.dto.UserResponse;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AuthResponse {
    private String accessToken;
    private String tokenType;
    private Long expiresIn;
    private UserResponse user;
}
