package com.techhub.app.aiservice.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "chatbot")
@Data
public class ChatbotProperties {
    private Embedding embedding = new Embedding();
    private boolean mockEmbeddings;
    private String systemPrompt;

    @Data
    public static class Embedding {
        private String modelId;
        private int maxSources;
    }
}
