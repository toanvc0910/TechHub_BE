package com.techhub.app.proxyclient.business.auth.service;

import com.techhub.app.proxyclient.business.auth.model.request.AuthenticationRequest;
import com.techhub.app.proxyclient.business.auth.model.response.AuthRespDto;
import com.techhub.app.proxyclient.business.auth.model.response.AuthenticationResponse;
import com.techhub.app.proxyclient.business.auth.model.request.GetTokenDto;
import com.techhub.app.proxyclient.business.auth.model.request.RefreshTokenReqDto;
import com.techhub.app.proxyclient.business.auth.model.response.GetTokenRespDto;

public interface AuthenticationService {
	
	AuthenticationResponse authenticate(final AuthenticationRequest authenticationRequest);
	Boolean authenticate(final String jwt);

	GetTokenRespDto authenticateInternal(final GetTokenDto authenticationRequest);
	AuthRespDto authenticate(String grantType, String clientId, String clientSecret);

	GetTokenRespDto refreshToken(final RefreshTokenReqDto reqDto);
	
}
