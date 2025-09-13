package com.techhub.app.proxyclient.jwt.service.impl;

import com.techhub.app.proxyclient.business.user.model.UserDto;
import com.techhub.app.proxyclient.business.user.service.UserClient2Service;
import com.techhub.app.proxyclient.business.user.service.UserClientService;
import com.techhub.app.proxyclient.exception.wrapper.InvalidCredentialException;
import com.techhub.app.proxyclient.jwt.domain.AuthUserDetails;
import com.techhub.app.proxyclient.jwt.domain.CustomUserDetails;
import lombok.extern.slf4j.Slf4j;
import org.common.dbiz.dto.userDto.AuthDto;
import org.common.dbiz.payload.GlobalReponse;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;

@Slf4j
@Service
public class CustomUserDetailsService implements com.techhub.app.proxyclient.jwt.service.CustomUserDetailsService {
    private final UserClientService userClientService;
    private final UserClient2Service userClient2Service;

    public CustomUserDetailsService(UserClientService userClientService, UserClient2Service userClient2Service) {
        this.userClientService = userClientService;
        this.userClient2Service = userClient2Service;
    }

    @Override
    public UserDetails loadUserByUsernameAndTenantId(String username, Integer tenantId) throws UsernameNotFoundException {
        Integer newTenantId =  0;
        if (username != null && username.equals("WebService")){
            newTenantId = tenantId;
            tenantId = 0;
        }
        LocalDateTime startTime = LocalDateTime.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");
        log.info("Begin get user proxy at {}", startTime.format(formatter));
        UserDto user =  this.userClientService.findByUsername(tenantId,username,newTenantId).getBody();
        LocalDateTime endTime = LocalDateTime.now();
        log.info("Begin get user proxy at {}", endTime.format(formatter));

        if(user != null)
        {
            return new CustomUserDetails(
                    user.getTenantId(),
                    user.getUserName(),
                    user.getPassword(),
                    user.getUserId(),
                    Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER"))
            );
        }else{
            throw new UsernameNotFoundException("User not found with username: " + username);
        }
    }

    @Override
    public UserDetails loadUserByUsernameAndTenantIdInternal(String username,String password,
                                                             Integer tenantId,Integer userId) {
        return new CustomUserDetails(
                tenantId,username,password,userId,
                Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER"))
        );
    }

    @Override
    public UserDetails loadByClientIdAndClientSecretAndGrantType(String clientId, String clientSecret, String grantType) {

        AuthDto authDto = AuthDto.builder()
                .clientId(clientId)
                .clientSecret(clientSecret)
                .grantType(grantType)
                .build();
        GlobalReponse dto =  this.userClient2Service.authentication(
                authDto
        ).getBody();


        if(dto.getStatus().intValue() == HttpStatus.OK.value())
        {
            return new AuthUserDetails(
                    authDto,
                    Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER"))
            );
        }else{
            throw new InvalidCredentialException();
        }

    }

    @Override
    public UserDetails loadByClientId(String clientId) {
        AuthDto authDto = AuthDto.builder()
                .clientId(clientId)
                .build();
        GlobalReponse dto =  this.userClient2Service.authentication(
                authDto
        ).getBody();


        if(dto.getStatus().intValue() == HttpStatus.OK.value())
        {
            return new AuthUserDetails(
                    authDto,
                    Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER"))
            );
        }else{
            return null;
        }
    }

    @Override
    public UserDetails loadUserByUsername(String userName) throws UsernameNotFoundException {
        UserDto user =  this.userClientService.findByUsername(userName).getBody();

        if(user != null)
        {
            return new CustomUserDetails(
                    user.getTenantId(),
                    user.getUserName(),
                    user.getPassword(),
                    user.getUserId(),
                    Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER"))
            );
        }else{
            throw new UsernameNotFoundException("User not found with username: " + userName);
        }


    }
}