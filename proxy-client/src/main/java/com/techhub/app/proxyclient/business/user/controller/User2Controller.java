package com.techhub.app.proxyclient.business.user.controller;

import com.techhub.app.proxyclient.business.user.service.UserClient2Service;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.common.dbiz.dto.userDto.UserDto;
import org.common.dbiz.dto.userDto.password.ChangePasswordDto;
import org.common.dbiz.dto.userDto.password.EmailDto;
import org.common.dbiz.dto.userDto.password.VerifyCodeDto;
import org.common.dbiz.payload.GlobalReponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v2/users")
@RequiredArgsConstructor
@Slf4j
public class User2Controller {

    private final UserClient2Service userClientService;


    @PostMapping("/login")
    public ResponseEntity<GlobalReponse> Login(
            @RequestBody
             final UserDto userLoginDto) {
        log.info("*** UserDto, resource; login user ***");
        return ResponseEntity.ok(this.userClientService.Login(userLoginDto)).getBody();
    }

    @PostMapping("/register")
    public ResponseEntity<GlobalReponse> register(
            @RequestBody
            UserDto userDto) {
        log.info("*** UserDto, resource; register user ***");
        return ResponseEntity.ok(this.userClientService.register(userDto)).getBody();
    }

    @PostMapping("/sendVerifyEmail")
    public ResponseEntity<GlobalReponse> sendVerifyEmail(
            @RequestBody
            EmailDto email) {
        log.info("*** UserDto, resource; send verify email ***");
        return ResponseEntity.ok(this.userClientService.sendVerifyEmail(email)).getBody();
    }

    @PostMapping("/verifyEmail")
    public ResponseEntity<GlobalReponse> verifyEmail(
            @RequestBody
            VerifyCodeDto code) {
        log.info("*** UserDto, resource; verify email ***");
        return ResponseEntity.ok(this.userClientService.verifyEmail(code)).getBody();
    }

    @PostMapping("/cancelVerifyEmail")
    public ResponseEntity<GlobalReponse> cancelVerifyEmail(
            @RequestBody
            EmailDto email) {
        log.info("*** UserDto, resource; cancel verify email ***");
        return ResponseEntity.ok(this.userClientService.cancelVerifyEmail(email)).getBody();
    }

    @PostMapping("/changePassword")
    public ResponseEntity<GlobalReponse> changePassword(
            @RequestBody
            ChangePasswordDto code) {
        log.info("*** UserDto, resource; change password ***");
        return ResponseEntity.ok(this.userClientService.changePassword(code)).getBody();
    }

    @PostMapping("/forgotPassword")
    public ResponseEntity<GlobalReponse> forgotPassword(
            @RequestBody
            ChangePasswordDto code) {
        log.info("*** UserDto, resource; fotgot password ***");
        return ResponseEntity.ok(this.userClientService.forgotPassword(code)).getBody();
    }

    @PostMapping("/update")
    public ResponseEntity<GlobalReponse> update(
            @RequestBody
            UserDto dto) {
        log.info("*** UserDto, resource; Update User ***");
        return ResponseEntity.ok(this.userClientService.update(dto)).getBody();
    }
}










