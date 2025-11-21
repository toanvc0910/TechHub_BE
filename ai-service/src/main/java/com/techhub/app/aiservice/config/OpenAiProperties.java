package com.techhub.app.aiservice.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "openai")
@Data
public class OpenAiProperties {
    private String baseUrl;
    private String apiKey;
    private Chat chat = new Chat();

    @Data
    public static class Chat {
        private String model;
        private int maxOutputTokens;
    }
}
