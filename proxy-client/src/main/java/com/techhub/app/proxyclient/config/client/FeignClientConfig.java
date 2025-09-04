package com.techhub.app.proxyclient.config.client;

import feign.RequestInterceptor;
import feign.RequestTemplate;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;


@Configuration
public class FeignClientConfig {
    private final JwtUtil jwtUtil;

    public FeignClientConfig(JwtUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
    }

    @Bean
    public RequestInterceptor requestInterceptor() {
        return new RequestInterceptor() {
            public void apply(RequestTemplate template) {
                ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();

                if (attributes != null) {
//                    HttpServletRequest request = attributes.getRequest();
                    String authorizationToken = attributes.getRequest().getHeader("Authorization");
                    if (authorizationToken != null) {
                        authorizationToken.startsWith("Bearer ");
                        String token =  authorizationToken.substring(7);
                        template.header("userId", jwtUtil.extractDuserID(token).toString());
                        template.header("createBy", jwtUtil.extractUsername(token));
                        template.header("updateBy", jwtUtil.extractUsername(token));
                        template.header("Accept-Language", jwtUtil.extractLanguage(token));
                    }
                }

            }
        };
    }

}