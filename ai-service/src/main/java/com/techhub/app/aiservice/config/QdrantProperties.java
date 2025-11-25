package com.techhub.app.aiservice.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "qdrant")
@Data
public class QdrantProperties {
    private String host;
    private String apiKey;
    private String recommendationCollection;
    private String lessonCollection;
    private String profileCollection;
}
