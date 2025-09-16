package com.techhub.app.proxyclient.jwt.service.impl;

import com.techhub.app.proxyclient.business.auth.model.response.AuthRespDto;
import com.techhub.app.proxyclient.jwt.domain.AuthUserDetails;
import com.techhub.app.proxyclient.jwt.service.JwtService;
import com.techhub.app.proxyclient.jwt.util.JwtUtil;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.function.Function;

@Service
@Slf4j
@RequiredArgsConstructor
public class JwtServiceImpl implements JwtService {
	
	private final JwtUtil jwtUtil;
	
	@Override
	public String extractUsername(final String token) {
		log.info("**String, jwt service extract username from given token!*");
		return this.jwtUtil.extractUsername(token);
	}
	
	@Override
	public Date extractExpiration(final String token) {
		log.info("**Date, jwt service extract expiration from given token!*");
		return this.jwtUtil.extractExpiration(token);
	}

	@Override
	public Date extractIssuedAt(final String token) {
		log.info("**Date, jwt service extract issuedAt from given token!*");
		return this.jwtUtil.extractIssuedAt(token);
	}

	@Override
	public <T> T extractClaims(final String token, final Function<Claims, T> claimsResolver) {
		log.info("**T, jwt service extract claims from given token and claimResolver Function!*");
		return this.jwtUtil.extractClaims(token, claimsResolver);
	}
	
	@Override
	public String generateAccessToken(final UserDetails userDetails){
		log.info("**String, jwt service generate access token from given userDetails!*");
		return this.jwtUtil.generateAccessToken(userDetails);
	}

	@Override
	public String generateRefreshToken(final UserDetails userDetails){
		log.info("**String, jwt service generate refresh token from given userDetails!*");
		return this.jwtUtil.generateRefreshToken(userDetails);
	}

	@Override
	public String refreshExpiredToken(final String expiredToken) {
		log.info("**String, jwt service refresh token from given expired token!*");
		return this.jwtUtil.refreshExpiredToken(expiredToken);
	}
	
	@Override
	public Boolean validateToken(final String token, final UserDetails userDetails) {
		log.info("**Boolean, jwt service validate token from given token and userDetails!*");
		return this.jwtUtil.validateToken(token, userDetails);
	}

	@Override
	public AuthRespDto generateToken(AuthUserDetails userDetails) {
		return this.jwtUtil.generateToken(userDetails);
	}

	@Override
	public String extractIssuer(String token) {
		return this.jwtUtil.extractIssuer(token);
	}


}
