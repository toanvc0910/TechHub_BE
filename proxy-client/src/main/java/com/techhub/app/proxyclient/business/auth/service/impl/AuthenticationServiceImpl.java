package com.techhub.app.proxyclient.business.auth.service.impl;

import com.techhub.app.proxyclient.business.auth.model.request.AuthenticationRequest;
import com.techhub.app.proxyclient.business.auth.model.response.AuthRespDto;
import com.techhub.app.proxyclient.business.auth.model.response.AuthenticationResponse;
import com.techhub.app.proxyclient.business.auth.service.AuthenticationService;
import com.techhub.app.proxyclient.business.user.service.TokenClientService;
import com.techhub.app.proxyclient.business.user.service.UserClientService;
import com.techhub.app.proxyclient.exception.wrapper.IllegalAuthenticationCredentialsException;
import com.techhub.app.proxyclient.jwt.domain.AuthUserDetails;
import com.techhub.app.proxyclient.jwt.domain.CustomUserDetails;
import com.techhub.app.proxyclient.jwt.domain.CustomUsernamePasswordAuthenticationToken;
import com.techhub.app.proxyclient.jwt.service.JwtService;
import com.techhub.app.proxyclient.jwt.service.impl.CustomUserDetailsService;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.SignatureException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.common.dbiz.dto.userDto.GetTokenDto;
import org.common.dbiz.dto.userDto.GetTokenRespDto;
import org.common.dbiz.dto.userDto.TokenDto;
import org.common.dbiz.dto.userDto.request.RefreshTokenReqDto;
import org.common.dbiz.exception.PosException;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.stereotype.Service;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

@Service
@Slf4j
@RequiredArgsConstructor
public class AuthenticationServiceImpl implements AuthenticationService {
	
	private final AuthenticationManager authenticationManager;
	private final CustomUserDetailsService userDetailsService;
	private final JwtService jwtService;
	private final UserClientService userClientService;
	private final TokenClientService tokenClientService;

	@Override
	public AuthenticationResponse authenticate(final AuthenticationRequest authenticationRequest) {
		
		log.info("** AuthenticationResponse, authenticate user service*\n");

		try {
			this.authenticationManager.authenticate(new CustomUsernamePasswordAuthenticationToken(
					authenticationRequest.getUsername(), authenticationRequest.getPassword(), authenticationRequest.getDTenantId()));
		}
		catch (BadCredentialsException e) {
			throw new IllegalAuthenticationCredentialsException("#### Bad credentials! ####");
		}
		
			return new AuthenticationResponse(this.jwtService.generateAccessToken(
					(CustomUserDetails)this.userDetailsService.loadUserByUsernameAndTenantId(
							authenticationRequest.getUsername(),
							authenticationRequest.getDTenantId()),
					authenticationRequest.getDOrgId(),
					authenticationRequest.getLanguage()
			));
	}
	
	@Override
	public Boolean authenticate(final String jwt) {
		return null;
	}

	@Override
	public GetTokenRespDto authenticateInternal(GetTokenDto authenticationRequest) {

		CustomUserDetails userDetails = (CustomUserDetails) this.userDetailsService.loadUserByUsernameAndTenantIdInternal(
				authenticationRequest.getUsername(),
				authenticationRequest.getPassword(),
				authenticationRequest.getD_tenant_id(),
				authenticationRequest.getUserId());
		Integer orgId = authenticationRequest.getD_org_id();
		String language = authenticationRequest.getLanguage();

		String accessToken = this.jwtService.generateAccessToken(userDetails, orgId, language);
		String refreshToken = this.jwtService.generateRefreshToken(userDetails, orgId, language);

//        // Tính theo giờ quốc tế UTC -> chậm hơn 7h so với VN
//		String issued = DateTimeFormatter.ISO_INSTANT
//				.format(jwtService.extractIssuedAt(refreshToken).toInstant());
//		String expireAt = DateTimeFormatter.ISO_INSTANT
//				.format(jwtService.extractExpiration(refreshToken).toInstant());

        // Chuyển về giờ VN
        String issued = DateTimeFormatter.ISO_LOCAL_DATE_TIME
                .format(jwtService.extractIssuedAt(refreshToken).toInstant()
						.atZone(ZoneId.of("Asia/Ho_Chi_Minh"))
						.toLocalDateTime()
				);
        String expireAt = DateTimeFormatter.ISO_LOCAL_DATE_TIME
                .format(jwtService.extractExpiration(refreshToken).toInstant()
						.atZone(ZoneId.of("Asia/Ho_Chi_Minh"))
						.toLocalDateTime()
				);

//		Map<String,String> headers = new HashMap<>();
//		headers.put("Authorization", "Bearer " + accessToken);

		tokenClientService.save(authenticationRequest.getD_tenant_id(), TokenDto.builder()
				.orgId(orgId)
				.userId(userDetails.getUserId())
				.refreshToken(refreshToken)
				.issued(issued)
				.expireAt(expireAt)
				.build()
		);

		return new GetTokenRespDto(accessToken, refreshToken
//				, this.jwtService.extractExpiration(accessToken).toString()
		);
	}

	@Override
	public AuthRespDto authenticate(String grantType, String clientId, String clientSecret) {


		log.info("** Authentication, authenticate user service*\n");

//		try {
//			this.authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(clientId, clientSecret));
//		}
//		catch (BadCredentialsException e) {
//			throw new InvalidCredentialException();
//		}

		return this.jwtService.generateToken((AuthUserDetails)this.userDetailsService
				.loadByClientIdAndClientSecretAndGrantType(clientId,clientSecret,grantType));
	}

	@Override
	public GetTokenRespDto refreshToken(RefreshTokenReqDto reqDto) {

		String refreshToken = reqDto.getRefreshToken();
		Integer tenantId;

		try {
			tenantId = jwtService.extractTenantId(refreshToken);
		} catch (ExpiredJwtException e) {
			tokenClientService.revokeRefreshToken(
					jwtService.extractdTenantIdExpired(refreshToken),
					TokenDto.builder().refreshToken(refreshToken).build());
			throw new PosException("Refresh token expired. Revoked token in db");
		} catch (SignatureException | MalformedJwtException e) {
			throw new PosException("Refresh token has wrong signature/is malformed");
		}

		TokenDto tokenDto = tokenClientService.checkValidRefreshToken(
				tenantId,
				TokenDto.builder().refreshToken(refreshToken).build())
				.getBody();

		if ("N".equals(tokenDto.getIsValid())) {
			throw new PosException("Refresh token revoked");
		}

		String newAccessToken;
		try {
			newAccessToken = this.jwtService.refreshExpiredToken(reqDto.getAccessToken());
		} catch (SignatureException | MalformedJwtException e) {
			throw new PosException("Access token invalid");
		}

		return GetTokenRespDto.builder()
				.accessToken(newAccessToken)
				.refreshToken(refreshToken)
				.build();
	}

}










