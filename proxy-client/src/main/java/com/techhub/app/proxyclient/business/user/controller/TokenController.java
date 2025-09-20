package com.techhub.app.proxyclient.business.user.controller;

import com.techhub.app.proxyclient.business.user.service.TokenClientService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(value = {"/api/v1/token"})
@Slf4j
@RequiredArgsConstructor
public class TokenController {

    private final TokenClientService tokenClientService;

//    @PostMapping("/save")
//    public ResponseEntity<GlobalReponse> save(@RequestBody @Valid final TokenDto dto) {
//        log.info("*** TokenDto, controller; save token ***");
//        return ResponseEntity.ok(this.tokenClientService.save(dto).getBody());
//    }
//
//    @GetMapping("/check")
//    public ResponseEntity<TokenDto> checkValidRefreshToken(@RequestBody @Valid final TokenDto dto) {
//        log.info("*** TokenDto, controller; check refresh token valid ***");
//        return ResponseEntity.ok(this.tokenClientService.checkValidRefreshToken(dto).getBody());
//    }
//
//    @PostMapping("/revoke")
//    public ResponseEntity<GlobalReponse> revokeRefreshToken(@RequestBody @Valid final TokenDto dto) {
//        log.info("*** TokenDto, controller; revoke refresh token ***");
//        return ResponseEntity.ok(this.tokenClientService.revokeRefreshToken(dto).getBody());
//    }

}
