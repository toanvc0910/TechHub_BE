package com.techhub.app.proxyclient.business.user.service;

import org.common.dbiz.dto.userDto.AuthDto;
import org.common.dbiz.dto.userDto.UserDto;
import org.common.dbiz.dto.userDto.password.ChangePasswordDto;
import org.common.dbiz.dto.userDto.password.EmailDto;
import org.common.dbiz.dto.userDto.password.VerifyCodeDto;
import org.common.dbiz.payload.GlobalReponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "USER-SERVICE", contextId = "userClient2Service", path = "/user-service/api/v2/users", decode404 = true)
public interface UserClient2Service {

    @PostMapping("/login")
    public ResponseEntity<GlobalReponse> Login(
            @RequestBody
         final UserDto userLoginDto);

    @PostMapping("/register")
    public ResponseEntity<GlobalReponse> register(
            @RequestBody
            UserDto userDto);


    @PostMapping("/sendVerifyEmail")
    public ResponseEntity<GlobalReponse> sendVerifyEmail(
            @RequestBody
            EmailDto email);

    @PostMapping("/verifyEmail")
    public ResponseEntity<GlobalReponse> verifyEmail(
            @RequestBody
            VerifyCodeDto code);

    @PostMapping("/cancelVerifyEmail")
    public ResponseEntity<GlobalReponse> cancelVerifyEmail(
            @RequestBody
            EmailDto email);

    @PostMapping("/changePassword")
    public ResponseEntity<GlobalReponse> changePassword(
            @RequestBody
            ChangePasswordDto code);

    @PostMapping("/forgotPassword")
    public ResponseEntity<GlobalReponse> forgotPassword(
            @RequestBody
            ChangePasswordDto code);

    @PostMapping("/update")
    public ResponseEntity<GlobalReponse> update(
            @RequestBody
            UserDto dto);


    @PostMapping("/auth")
    ResponseEntity<GlobalReponse> authentication(
            @RequestBody AuthDto userDto
    );
}
