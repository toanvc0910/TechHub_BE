package com.techhub.app.commonservice.websocket.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Configuration properties for WebSocket/STOMP.
 * Được đọc từ application.yml với prefix "app.websocket"
 */
@Data
@ConfigurationProperties(prefix = "app.websocket")
public class WebSocketProperties {

    /**
     * STOMP endpoint exposed to clients for the initial handshake.
     * Default: /ws-comment
     */
    private String endpoint = "/ws-comment";

    /**
     * Prefixes that clients use when sending messages to application @MessageMapping handlers.
     * Default: /app
     */
    private List<String> applicationDestinationPrefixes = new ArrayList<>(List.of("/app"));

    /**
     * Simple broker prefixes that clients subscribe to for server push updates.
     * Default: /topic, /queue
     */
    private List<String> brokerDestinationPrefixes = new ArrayList<>(Arrays.asList("/topic", "/queue"));

    /**
     * Prefix used for user-specific messaging.
     * Cho phép gửi message đến user cụ thể: /user/{userId}/queue/...
     * Default: /user
     */
    private String userDestinationPrefix = "/user";

    /**
     * Allowed origins for WebSocket handshake requests.
     * Nếu rỗng thì cho phép tất cả origins (*)
     */
    private List<String> allowedOrigins = new ArrayList<>();

    /**
     * Toggle SockJS fallback support for environments where native WebSocket is not available.
     * Default: true
     */
    private boolean sockJsEnabled = true;

    /**
     * Preserve message publish order.
     * Default: true
     */
    private boolean preservePublishOrder = true;

    /**
     * Enable external message broker (RabbitMQ STOMP relay).
     * Nếu false, sử dụng Simple Broker (in-memory).
     * Default: false
     */
    private boolean relayEnabled = false;

    /**
     * RabbitMQ STOMP relay configuration.
     */
    private RelayProperties relay = new RelayProperties();

    @Data
    public static class RelayProperties {
        /**
         * STOMP relay host (e.g. RabbitMQ host).
         */
        private String host = "localhost";

        /**
         * STOMP relay port (RabbitMQ default: 61613).
         */
        private int port = 61613;

        /**
         * Login credential for the relay (RabbitMQ default: guest).
         */
        private String login = "guest";

        /**
         * Passcode credential for the relay (RabbitMQ default: guest).
         */
        private String passcode = "guest";

        /**
         * Virtual host for RabbitMQ.
         */
        private String virtualHost = "/";

        /**
         * System login for relay (optional, if different from client login).
         */
        private String systemLogin;

        /**
         * System passcode for relay.
         */
        private String systemPasscode;

        /**
         * Heartbeat send interval in milliseconds.
         */
        private int systemHeartbeatSendInterval = 10000;

        /**
         * Heartbeat receive interval in milliseconds.
         */
        private int systemHeartbeatReceiveInterval = 10000;
    }
}
