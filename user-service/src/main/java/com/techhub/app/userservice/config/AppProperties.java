package com.techhub.app.userservice.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "app")
public class AppProperties {

    private final OAuth2 oauth2 = new OAuth2();
    private final Mail mail = new Mail();

    @Data
    public static class OAuth2 {
        private String authorizedRedirectUris = "http://localhost:3000/oauth2/redirect";
    }

    @Data
    public static class Mail {
        private String fromEmail = "trungphandinh340@gmail.com";
        private String fromName = "TechHub Team";
    }
}
