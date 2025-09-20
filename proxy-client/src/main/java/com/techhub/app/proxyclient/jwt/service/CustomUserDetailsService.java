package com.techhub.app.proxyclient.jwt.service;

import com.techhub.app.proxyclient.jwt.domain.AuthUserDetails;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;

public interface CustomUserDetailsService  extends UserDetailsService {
        UserDetails loadUserByUsernameAndTenantId(String username, Integer tenantId);

        UserDetails loadUserByUsernameAndTenantIdInternal(String username,String password,
                                                          Integer tenantId,Integer userId);

        AuthUserDetails loadByClientIdAndClientSecretAndGrantType(String clientId, String clientSecret, String grantType);
        AuthUserDetails loadByClientId(String clientId);
}
