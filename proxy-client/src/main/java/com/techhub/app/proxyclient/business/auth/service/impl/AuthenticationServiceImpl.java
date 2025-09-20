package com.techhub.app.proxyclient.business.auth.service.impl;

import com.techhub.app.proxyclient.business.auth.model.request.AuthenticationRequest;
import com.techhub.app.proxyclient.business.auth.model.request.GetTokenDto;
import com.techhub.app.proxyclient.business.auth.model.request.RefreshTokenReqDto;
import com.techhub.app.proxyclient.business.auth.model.response.AuthRespDto;
import com.techhub.app.proxyclient.business.auth.model.response.AuthenticationResponse;
import com.techhub.app.proxyclient.business.auth.model.response.GetTokenRespDto;
import com.techhub.app.proxyclient.business.auth.service.AuthenticationService;
import com.techhub.app.proxyclient.exception.wrapper.IllegalAuthenticationCredentialsException;
import com.techhub.app.proxyclient.jwt.domain.AuthUserDetails;
import com.techhub.app.proxyclient.jwt.service.JwtService;
import com.techhub.app.proxyclient.jwt.service.impl.CustomUserDetailsService;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.SignatureException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class AuthenticationServiceImpl implements AuthenticationService {
	
	private final AuthenticationManager authenticationManager;
	private final CustomUserDetailsService userDetailsService;
	private final JwtService jwtService;

	@Override
	public AuthenticationResponse authenticate(final AuthenticationRequest authenticationRequest) {
		log.info("** AuthenticationResponse, authenticate user service*\n");
		try {
			this.authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(
					authenticationRequest.getUsername(), authenticationRequest.getPassword()));
		}
		catch (BadCredentialsException e) {
			throw new IllegalAuthenticationCredentialsException("#### Bad credentials! ####");
		}

		UserDetails userDetails = this.userDetailsService.loadUserByUsername(
				authenticationRequest.getUsername());
		return new AuthenticationResponse(this.jwtService.generateAccessToken(userDetails));
	}
	
	@Override
	public Boolean authenticate(final String jwt) {
		try {
			jwtService.extractUsername(jwt);
			return true;
		} catch (Exception ex) {
			return false;
		}
	}

	@Override
	public GetTokenRespDto authenticateInternal(GetTokenDto authenticationRequest) {
		UserDetails userDetails = this.userDetailsService.loadUserByUsernameAndTenantIdInternal(
				authenticationRequest.getUsername(),
				authenticationRequest.getPassword(),
				0,
				authenticationRequest.getUserId());

		String accessToken = this.jwtService.generateAccessToken(userDetails);
		String refreshToken = this.jwtService.generateRefreshToken(userDetails);

		return new GetTokenRespDto(accessToken, refreshToken);
	}

	@Override
	public AuthRespDto authenticate(String grantType, String clientId, String clientSecret) {
		log.info("** Authentication, authenticate client credentials flow*\n");
		return this.jwtService.generateToken(this.userDetailsService
				.loadByClientIdAndClientSecretAndGrantType(clientId,clientSecret,grantType));
	}

	@Override
	public GetTokenRespDto refreshToken(RefreshTokenReqDto reqDto) {
		String refreshToken = reqDto.getRefreshToken();
		try {
			// Validate refresh token structure/signature
			jwtService.extractUsername(refreshToken);
		} catch (ExpiredJwtException e) {
			// Accept expired refresh token? For simplicity, reject
			throw new IllegalAuthenticationCredentialsException("Refresh token expired");
		} catch (SignatureException | MalformedJwtException e) {
			throw new IllegalAuthenticationCredentialsException("Refresh token invalid");
		}

		String newAccessToken;
		try {
			newAccessToken = this.jwtService.refreshExpiredToken(reqDto.getAccessToken());
		} catch (SignatureException | MalformedJwtException e) {
			throw new IllegalAuthenticationCredentialsException("Access token invalid");
		}

		return GetTokenRespDto.builder()
				.accessToken(newAccessToken)
				.refreshToken(refreshToken)
				.build();
	}
}
