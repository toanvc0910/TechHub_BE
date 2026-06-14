package com.techhub.app.proxyclient.config;

import io.netty.channel.ChannelOption;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;
import reactor.netty.resources.ConnectionProvider;

import java.time.Duration;

@Configuration
public class RestTemplateConfig {

    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }

    /**
     * LoadBalanced WebClient.Builder for service discovery via Eureka.
     * Use service names like "http://AI-SERVICE/api/..." instead of hardcoded URLs.
     *
     * <p>The connection pool is tuned for the long-lived SSE streams used by
     * {@code AiStreamingProxyController}. reactor-netty otherwise keeps idle
     * keep-alive connections in the pool indefinitely; uvicorn (the AI service)
     * closes keep-alive connections after ~5s idle, so a reused stale connection
     * fails with {@code PrematureCloseException: Connection prematurely closed
     * BEFORE response}. We evict idle connections well before that window and
     * give SSE responses a generous read timeout. Retrying is intentionally NOT
     * configured because the stream POST has side effects (creates a chat
     * session and persists messages) and a retry would duplicate them.
     */
    @Bean
    @LoadBalanced
    public WebClient.Builder webClientBuilder() {
        ConnectionProvider connectionProvider = ConnectionProvider.builder("ai-stream-pool")
                .maxIdleTime(Duration.ofSeconds(2))      // evict before uvicorn's 5s keep-alive timeout
                .maxLifeTime(Duration.ofSeconds(55))
                .pendingAcquireTimeout(Duration.ofSeconds(10))
                .evictInBackground(Duration.ofSeconds(10))
                .build();

        HttpClient httpClient = HttpClient.create(connectionProvider)
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 5000)
                .responseTimeout(Duration.ofMinutes(5))  // SSE streams can run long
                .keepAlive(true);

        return WebClient.builder()
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .codecs(configurer -> configurer
                        .defaultCodecs()
                        .maxInMemorySize(16 * 1024 * 1024)); // 16MB buffer
    }
}
