package com.techhub.app.userservice.service;

import com.techhub.app.userservice.dto.request.LoginRequest;
import com.techhub.app.userservice.dto.request.RefreshTokenRequest;
import com.techhub.app.userservice.dto.request.SaveRefreshTokenRequest;
import com.techhub.app.userservice.dto.response.AuthResponse;

public interface AuthService {

    AuthResponse authenticate(LoginRequest request);

    void logout(String token);

    AuthResponse refreshToken(RefreshTokenRequest request);

    void saveRefreshToken(SaveRefreshTokenRequest request);
}
