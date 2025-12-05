package com.techhub.app.userservice.dto.request;

import com.techhub.app.userservice.enums.AuthProviderEnum;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SaveRefreshTokenRequest {

    @NotNull(message = "User ID is required")
    private UUID userId;

    @NotNull(message = "Provider is required")
    private AuthProviderEnum provider;

    @NotBlank(message = "Refresh token is required")
    private String refreshToken;

    @NotNull(message = "Expires at is required")
    private LocalDateTime expiresAt;
}
