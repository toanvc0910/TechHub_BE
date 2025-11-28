package com.techhub.app.blogservice.websocket.config;

import com.techhub.app.commonservice.websocket.config.WebSocketProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

/**
 * WebSocket Configuration cho Blog Service.
 * Cấu hình STOMP over WebSocket cho real-time comments.
 */
@Slf4j
@Configuration
@EnableWebSocketMessageBroker
@EnableConfigurationProperties(WebSocketProperties.class)
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private final WebSocketProperties webSocketProperties;
    private final WebSocketAuthInterceptor webSocketAuthInterceptor;

    @Bean
    public TaskScheduler webSocketTaskScheduler() {
        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
        scheduler.setPoolSize(1);
        scheduler.setThreadNamePrefix("ws-heartbeat-");
        scheduler.initialize();
        return scheduler;
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        String endpoint = webSocketProperties.getEndpoint();
        String[] allowedOrigins = webSocketProperties.getAllowedOrigins().isEmpty()
                ? new String[]{"*"}
                : webSocketProperties.getAllowedOrigins().toArray(new String[0]);

        // Native WebSocket endpoint
        registry.addEndpoint(endpoint)
                .setAllowedOriginPatterns(allowedOrigins);

        // SockJS fallback endpoint (nếu enabled)
        if (webSocketProperties.isSockJsEnabled()) {
            registry.addEndpoint(endpoint)
                    .setAllowedOriginPatterns(allowedOrigins)
                    .withSockJS();
        }

        log.info("WebSocket STOMP endpoint registered: {} (SockJS: {})", 
                endpoint, webSocketProperties.isSockJsEnabled());
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        // Application destination prefix - client gửi message tới @MessageMapping handlers
        registry.setApplicationDestinationPrefixes(
                webSocketProperties.getApplicationDestinationPrefixes().toArray(new String[0]));

        // User destination prefix - cho private messaging đến user cụ thể
        registry.setUserDestinationPrefix(webSocketProperties.getUserDestinationPrefix());

        // Configure message broker
        if (webSocketProperties.isRelayEnabled()) {
            // Use external STOMP broker (RabbitMQ)
            WebSocketProperties.RelayProperties relay = webSocketProperties.getRelay();
            registry.enableStompBrokerRelay(
                            webSocketProperties.getBrokerDestinationPrefixes().toArray(new String[0]))
                    .setRelayHost(relay.getHost())
                    .setRelayPort(relay.getPort())
                    .setClientLogin(relay.getLogin())
                    .setClientPasscode(relay.getPasscode())
                    .setSystemLogin(relay.getSystemLogin() != null ? relay.getSystemLogin() : relay.getLogin())
                    .setSystemPasscode(relay.getSystemPasscode() != null ? relay.getSystemPasscode() : relay.getPasscode())
                    .setSystemHeartbeatSendInterval(relay.getSystemHeartbeatSendInterval())
                    .setSystemHeartbeatReceiveInterval(relay.getSystemHeartbeatReceiveInterval())
                    .setVirtualHost(relay.getVirtualHost());

            log.info("STOMP Broker Relay enabled: {}:{}", relay.getHost(), relay.getPort());
        } else {
            // Use simple in-memory broker
            registry.enableSimpleBroker(
                            webSocketProperties.getBrokerDestinationPrefixes().toArray(new String[0]))
                    .setTaskScheduler(webSocketTaskScheduler())
                    .setHeartbeatValue(new long[]{10000, 10000});

            log.info("Simple Broker enabled with prefixes: {}", 
                    webSocketProperties.getBrokerDestinationPrefixes());
        }

        // Preserve message order
        registry.setPreservePublishOrder(webSocketProperties.isPreservePublishOrder());
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        // Register auth interceptor để xác thực JWT khi CONNECT
        registration.interceptors(webSocketAuthInterceptor);
    }
}
