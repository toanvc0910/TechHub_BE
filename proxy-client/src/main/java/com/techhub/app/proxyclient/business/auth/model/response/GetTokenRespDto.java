package com.techhub.app.proxyclient.business.auth.model.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GetTokenRespDto {
    private String accessToken;
    private String refreshToken;
}

