package com.techhub.app.proxyclient.jwt.service;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;

public interface CustomUserDetailsService  extends UserDetailsService {
        UserDetails loadUserByUsernameAndTenantId(String username, Integer tenantId);

        UserDetails loadUserByUsernameAndTenantIdInternal(String username,String password,
                                                          Integer tenantId,Integer userId);

        UserDetails loadByClientIdAndClientSecretAndGrantType(String clientId, String clientSecret, String grantType);
        UserDetails loadByClientId(String clientId);
}
