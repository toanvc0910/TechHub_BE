package com.techhub.app.proxyclient.business.auth.model.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RefreshTokenReqDto {
    private String accessToken;
    private String refreshToken;
}

