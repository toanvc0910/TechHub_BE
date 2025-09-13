package com.techhub.app.proxyclient.business.auth.controller;

import com.techhub.app.proxyclient.business.auth.model.request.AuthenticationRequest;
import com.techhub.app.proxyclient.business.auth.model.response.AuthenticationResponse;
import com.techhub.app.proxyclient.business.auth.service.AuthenticationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.common.dbiz.dto.userDto.GetTokenDto;
import org.common.dbiz.dto.userDto.GetTokenRespDto;
import org.common.dbiz.dto.userDto.request.RefreshTokenReqDto;
import org.springframework.boot.autoconfigure.couchbase.CouchbaseProperties;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

@RestController
@RequestMapping("/api/v1/authenticate")
@Slf4j
@RequiredArgsConstructor
//@CrossOrigin(origins = "https://dbizpos.digitalbiz.com.vn",methods = {RequestMethod.GET,RequestMethod.POST,RequestMethod.PUT,RequestMethod.PATCH,RequestMethod.DELETE,RequestMethod.OPTIONS},allowedHeaders ="https://dbizpos.digitalbiz.com.vn")
public class AuthenticationController {
	
	private final AuthenticationService authenticationService;
	
	@PostMapping
	public ResponseEntity<AuthenticationResponse> authenticate(
			@RequestBody 
			@NotNull(message = "") 
			@Valid final AuthenticationRequest authenticationRequest) {
		log.info("**Authentication controller, proceed with the request rebuild ---*\n");
		log.info("**Get token --*");
		return ResponseEntity.ok(this.authenticationService.authenticate(authenticationRequest));
	}
	
	@GetMapping("/jwt/{jwt}")
	public ResponseEntity<Boolean> authenticate(@PathVariable("jwt") final String jwt) {
		log.info("**Authentication controller, proceed with the request  rebuild ---*\n");
		CouchbaseProperties.Env env = new CouchbaseProperties.Env();
		return ResponseEntity.ok(this.authenticationService.authenticate(jwt));
	}

	@PostMapping("/internal")
	public ResponseEntity<GetTokenRespDto> authenticateInternal(
			@RequestBody
			@NotNull(message = "")
			@Valid final GetTokenDto authenticationRequest) {
		log.info("**Authentication controller, proceed with the request rebuild ---*\n");
		log.info("**Get token internal --*");
		return ResponseEntity.ok(this.authenticationService.authenticateInternal(authenticationRequest));
	}

	@PostMapping("/refresh")
	public ResponseEntity<GetTokenRespDto> refreshToken(
			@RequestBody
			@Valid final RefreshTokenReqDto reqDto) {

		log.info("**Authentication controller, proceed with the request rebuild ---*\n");
		log.info("**Refresh expired token --*");
		return ResponseEntity.ok(this.authenticationService.refreshToken(reqDto));

	}
}










