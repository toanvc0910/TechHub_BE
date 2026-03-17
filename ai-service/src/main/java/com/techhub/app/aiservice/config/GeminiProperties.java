package com.techhub.app.aiservice.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "gemini")
@Data
public class GeminiProperties {
    private String baseUrl = "https://generativelanguage.googleapis.com/v1beta";
    private String apiKey;
    private Chat chat = new Chat();

    @Data
    public static class Chat {
        private String model;
        private int maxOutputTokens;
    }
}
