package com.techhub.app.proxyclient.jwt.service;

import com.techhub.app.proxyclient.business.auth.model.response.AuthRespDto;
import com.techhub.app.proxyclient.jwt.domain.AuthUserDetails;
import com.techhub.app.proxyclient.jwt.domain.CustomUserDetails;
import io.jsonwebtoken.Claims;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Date;
import java.util.function.Function;

public interface JwtService {
	
	String extractUsername(final String token);
	Date extractExpiration(final String token);
	Date extractIssuedAt(final String token);
	Integer extractdTenantIdExpired(final String token);
	<T> T extractClaims(final String token, final Function<Claims, T> claimsResolver);

	String generateAccessToken(final CustomUserDetails userDetails, Integer dOrgId, String language);

	String generateRefreshToken(final CustomUserDetails userDetails, Integer dOrgId, String language);

	String refreshExpiredToken(final String expiredToken);

	Boolean validateToken(final String token, final UserDetails userDetails);

	Integer extractTenantId(final String token);

	AuthRespDto generateToken(final AuthUserDetails userDetails);

	String extractIssuer(final String token);
}










