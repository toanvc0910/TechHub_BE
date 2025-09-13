package com.techhub.app.proxyclient.business.auth.controller.v2;


import com.techhub.app.proxyclient.business.auth.model.response.AuthRespDto;
import com.techhub.app.proxyclient.business.auth.service.AuthenticationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v2/auth")
@Slf4j
@RequiredArgsConstructor
public class AuthController {
    private final AuthenticationService authenticationService;

    @PostMapping(value = "/token", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    public ResponseEntity<AuthRespDto> authenticate(
            @RequestParam(value = "grant_type") String grantType,
            @RequestParam(value = "client_id") String clientId,
            @RequestParam(value = "client_secret") String clientSecret){


        log.info("**Authentication controller, proceed with the request rebuild ---*\n");
        log.info("**Get token --*");
//        return ResponseEntity.ok(this.authenticationService.authenticate(authenticationRequest));

        log.info("grantType: {}", grantType);
        log.info("clientId: {}", clientId);
        log.info("clientSecret: {}", clientSecret);
        return ResponseEntity.ok( this.authenticationService.authenticate(grantType, clientId, clientSecret));
    }
}
