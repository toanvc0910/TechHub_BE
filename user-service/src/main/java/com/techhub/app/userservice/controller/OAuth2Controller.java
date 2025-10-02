package com.techhub.app.userservice.controller;

import com.techhub.app.commonservice.payload.GlobalResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/api/oauth2")
@RequiredArgsConstructor
@Slf4j
public class OAuth2Controller {

    @GetMapping("/success")
    public ResponseEntity<GlobalResponse<String>> oauth2Success(
            @RequestParam String token,
            @RequestParam String email,
            @RequestParam String username,
            @RequestParam String provider,
            HttpServletRequest httpRequest) {

        log.info("OAuth2 login success for user: {} via provider: {}", email, provider);

        return ResponseEntity.ok(
                GlobalResponse.success("OAuth2 login successful",
                    String.format("User %s logged in successfully via %s", username, provider))
                        .withPath(httpRequest.getRequestURI())
        );
    }

    @GetMapping("/error")
    public ResponseEntity<GlobalResponse<String>> oauth2Error(
            @RequestParam(required = false) String error,
            HttpServletRequest httpRequest) {

        log.error("OAuth2 login failed: {}", error);

        return ResponseEntity.badRequest().body(
                GlobalResponse.<String>error("OAuth2 login failed: " + (error != null ? error : "Unknown error"), 400)
                        .withPath(httpRequest.getRequestURI())
        );
    }
}
