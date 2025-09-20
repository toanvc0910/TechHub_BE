package com.techhub.app.proxyclient.jwt.util.impl;

import com.techhub.app.proxyclient.business.auth.model.response.AuthRespDto;
import com.techhub.app.proxyclient.jwt.domain.AuthUserDetails;
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
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

@Component
public class JwtUtilImpl implements JwtUtil {

    private static final String SECRET_KEY = "dbiz_company_secret_key";
    private static final String ISSUER = "dbiz";

    private static final long ACCESS_TOKEN_EXP_MS  = TimeUnit.HOURS.toMillis(12);
    private static final long REFRESH_TOKEN_EXP_MS = TimeUnit.DAYS.toMillis(30);

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
    public <T> T extractClaims(final String token, final Function<Claims, T> claimsResolver) {
        final Claims claims = this.extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    private Claims extractAllClaims(final String token) {
        return Jwts.parser().setSigningKey(SECRET_KEY).parseClaimsJws(token).getBody();
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

    private String createToken(final Map<String, Object> claims, final String subject, long validTime) {
        return Jwts.builder()
                .setClaims(claims)
                .setSubject(subject)
                .setIssuer(ISSUER)
                .setIssuedAt(new Date(System.currentTimeMillis()))
                .setExpiration(new Date(System.currentTimeMillis() + validTime))
                .signWith(SignatureAlgorithm.HS256, SECRET_KEY)
                .setHeaderParam("typ", "JWT")
                .compact();
    }

    @Override
    public String generateAccessToken(final UserDetails userDetails) {
        final Map<String, Object> claims = new HashMap<>();
        return this.createToken(claims, userDetails.getUsername(), ACCESS_TOKEN_EXP_MS);
    }

    @Override
    public String generateRefreshToken(final UserDetails userDetails) {
        final Map<String, Object> claims = new HashMap<>();
        return this.createToken(claims, "RefreshToken-" + userDetails.getUsername(), REFRESH_TOKEN_EXP_MS);
    }

    @Override
    public String refreshExpiredToken(final String expiredToken) {
        Claims oldClaims = extractAllClaimsExpired(expiredToken);
        Map<String,Object> newClaims = new HashMap<>();
        String username = oldClaims.getSubject();
        // If subject had RefreshToken- prefix, strip it
        if (username != null && username.startsWith("RefreshToken-")) {
            username = username.substring("RefreshToken-".length());
        }
        return this.createToken(newClaims, username, ACCESS_TOKEN_EXP_MS);
    }

    @Override
    public Boolean validateToken(final String token, final UserDetails userDetails) {
        final String username = this.extractUsername(token);
        return (username.equals(userDetails.getUsername()) && !isTokenExpired(token));
    }

    @Override
    public AuthRespDto generateToken(AuthUserDetails userDetails) {
        long expiresInMillis = TimeUnit.HOURS.toMillis(12);
        long expiresInSeconds = expiresInMillis / 1000;
        Date expirationDate = new Date(System.currentTimeMillis() + expiresInMillis);

        String token = Jwts.builder()
                .setSubject(userDetails.getUsername())
                .setIssuer(ISSUER)
                .setExpiration(expirationDate)
                .signWith(SignatureAlgorithm.HS256, SECRET_KEY)
                .compact();

        return AuthRespDto.builder()
                .accessToken(token)
                .tokenType("Bearer")
                .expiresIn(String.valueOf(expiresInSeconds))
                .build();
    }

    @Override
    public String extractIssuer(String token) {
        return this.extractClaims(token, Claims::getIssuer);
    }
}

