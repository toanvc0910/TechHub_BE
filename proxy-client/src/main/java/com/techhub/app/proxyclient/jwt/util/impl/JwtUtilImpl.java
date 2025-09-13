package com.techhub.app.proxyclient.jwt.util.impl;

import com.techhub.app.proxyclient.business.auth.model.response.AuthRespDto;
import com.techhub.app.proxyclient.jwt.domain.AuthUserDetails;
import com.techhub.app.proxyclient.jwt.domain.CustomUserDetails;
import com.techhub.app.proxyclient.jwt.util.JwtUtil;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

@Component
public class JwtUtilImpl implements JwtUtil {
	
	private static final String SECRET_KEY = "dbiz_company_secret_key";
	private static final String ISSUER = "dbiz";

	private static final long ACCESS_TOKEN_EXP_MS  = TimeUnit.DAYS.toMillis(1); 	// 1000L * 60 * 60 * 24; 		// 1 day
	private static final long REFRESH_TOKEN_EXP_MS = TimeUnit.DAYS.toMillis(30); 	// 1000L * 60 * 60 * 24 * 30; 	// 1 month

	@Override
	public Integer extractDuserID(String token) {
		return Optional.ofNullable(this.extractClaims(token, claims -> claims.get("dUserId", Integer.class)))
				.orElse(0);
	}

	@Override
	public Integer extractdTenantId(String token) {
		return Optional.ofNullable(this.extractClaims(token, claims -> claims.get("dTenantId", Integer.class)))
				.orElse(0);
	}
	@Override
	public Integer extractdTenantIdExpired(String token) {

		return Optional.ofNullable(this.extractClaimsExpired(token, claims -> claims.get("dTenantId", Integer.class)))
				.orElse(0);
	}


	@Override
	public Integer extractdOrgId(String token) {

		return Optional.ofNullable(this.extractClaims(token, claims -> claims.get("dOrgId", Integer.class)))
				.orElse(0);
	}
	@Override
	public String extractUsername(final String token) {
		return this.extractClaims(token, Claims::getSubject);
	}
	@Override
	public Date extractExpiration(final String token) {
		return this.extractClaims(token, Claims::getExpiration);
	}
	@Override
	public Date extractIssuedAt(final String token) {
		return this.extractClaims(token, Claims::getIssuedAt);
	}

	@Override
	public String extractLanguage(String token) {
		 return Optional.ofNullable(this.extractClaims(token, claims -> claims.get("language", String.class)))
				.orElse("en_US");
	}

	@Override
	public <T> T extractClaims(final String token, Function<Claims, T> claimsResolver) {
		final Claims claims = this.extractAllClaims(token);
		return claimsResolver.apply(claims);
	}
	
	private Claims extractAllClaims(final String token) {
		return Jwts.parser().setSigningKey(SECRET_KEY).parseClaimsJws(token).getBody();
	}

	private <T> T extractClaimsExpired(final String token, final Function<Claims, T> claimsResolver) {
		final Claims claims = this.extractAllClaimsExpired(token);
		return claimsResolver.apply(claims);
	}

	private Claims extractAllClaimsExpired(final String expiredToken) {
		try {
			return Jwts.parser()
					.setSigningKey(SECRET_KEY)
					.parseClaimsJws(expiredToken)
					.getBody();
		} catch (ExpiredJwtException e) {
			return e.getClaims();
		}
	}

	private Boolean isTokenExpired(final String token) {
		return this.extractExpiration(token).before(new Date());
	}
	
	@Override
	public String generateAccessToken(final CustomUserDetails userDetails, Integer dOrgId, String language) {
		final Map<String, Object> claims = new HashMap<>();
		claims.put("dTenantId", userDetails.getdTenantId());
		claims.put("dOrgId", dOrgId);
		claims.put("dUserId",userDetails.getUserId());
		claims.put("language",language);
		return this.createToken(claims, userDetails.getUsername(), ACCESS_TOKEN_EXP_MS );
	}

	@Override
	public String generateRefreshToken(final CustomUserDetails userDetails, Integer dOrgId, String language) {
		final Map<String, Object> claims = new HashMap<>();
		claims.put("dTenantId", userDetails.getdTenantId());
		claims.put("dOrgId", dOrgId);
		claims.put("dUserId",userDetails.getUserId());
		claims.put("language",language);
		return this.createToken(claims, "RefreshToken-" + userDetails.getUsername(), REFRESH_TOKEN_EXP_MS);
	}

	@Override
	public String refreshExpiredToken(final String expiredToken) {
		Claims oldClaims = extractAllClaimsExpired(expiredToken);

		Map<String,Object> newClaims = new HashMap<>();
		newClaims.put("dTenantId", 	oldClaims.get("dTenantId", Integer.class));
		newClaims.put("dOrgId", 	oldClaims.get("dOrgId",    Integer.class));
		newClaims.put("dUserId", 	oldClaims.get("dUserId",   Integer.class));
		newClaims.put("language", 	oldClaims.get("language",  String.class));
		String username = oldClaims.getSubject();

		return this.createToken(newClaims, username, ACCESS_TOKEN_EXP_MS);
	}
	
	private String createToken(final Map<String, Object> claims, final String subject, long validTime) {
		return Jwts.builder()
					.setClaims(claims)
					.setSubject(subject)
					.setIssuedAt(new Date(System.currentTimeMillis()))
//				.setExpiration(new Date(System.currentTimeMillis() + 1000 * 60 * 2)) // 2 phut
//				.setExpiration(new Date(System.currentTimeMillis() + 1000 * 60 * 60 * 10)) 10 tieng
				.setExpiration(new Date(System.currentTimeMillis() + validTime))
				.signWith(SignatureAlgorithm.HS256, SECRET_KEY)
				.setHeaderParam("typ", "JWT")
		.compact();
	}
	
	@Override
	public Boolean validateToken(final String token, final UserDetails userDetails) {
		final String username = this.extractUsername(token);
		return (
			username.equals(userDetails.getUsername()) && !isTokenExpired(token)
		);
	}

	@Override
	public AuthRespDto generateToken(AuthUserDetails userDetails) {
		return createToken(userDetails.getUsername());
	}

	@Override
	public String extractIssuer(String token) {
		return this.extractClaims(token, Claims::getIssuer);
	}

	private AuthRespDto createToken(final String subject) {


//		long expiresInMillis = 1000 * 60 * 5; // 5 phút
		long expiresInMillis = 1000 * 60  * 60 * 12; // 12h
		long expiresInSeconds = expiresInMillis / 1000;
		Date expirationDate = new Date(System.currentTimeMillis() + expiresInMillis);

		String token = Jwts.builder()
				.setSubject(subject)
//				.setClaims(new HashMap<>())
				.setIssuer(this.ISSUER)
				.setExpiration(expirationDate)
				.signWith(SignatureAlgorithm.HS256, SECRET_KEY)
				.compact();

		return AuthRespDto.builder()
				.accessToken(token)
				.tokenType("Bearer")
				.expiresIn(String.valueOf(expiresInSeconds))
				.build();
	}


}










