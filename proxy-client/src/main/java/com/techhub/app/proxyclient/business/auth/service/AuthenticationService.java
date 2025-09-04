package com.techhub.app.proxyclient.business.auth.service;

import com.techhub.app.proxyclient.business.auth.model.request.AuthenticationRequest;
import com.techhub.app.proxyclient.business.auth.model.response.AuthRespDto;
import com.techhub.app.proxyclient.business.auth.model.response.AuthenticationResponse;
import org.common.dbiz.dto.userDto.GetTokenDto;
import org.common.dbiz.dto.userDto.GetTokenRespDto;
import org.common.dbiz.dto.userDto.request.RefreshTokenReqDto;

public interface AuthenticationService {
	
	AuthenticationResponse authenticate(final AuthenticationRequest authenticationRequest);
	Boolean authenticate(final String jwt);

	GetTokenRespDto authenticateInternal(final GetTokenDto authenticationRequest);
	AuthRespDto authenticate(String grantType, String clientId, String clientSecret);

	GetTokenRespDto refreshToken(final RefreshTokenReqDto reqDto);
	
}
