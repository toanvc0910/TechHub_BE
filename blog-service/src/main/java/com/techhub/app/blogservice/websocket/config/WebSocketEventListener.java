package com.techhub.app.blogservice.websocket.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.*;

/**
 * WebSocket Event Listener for debugging
 * Logs all WebSocket connection events
 */
@Slf4j
@Component
public class WebSocketEventListener {

    @EventListener
    public void handleSessionConnect(SessionConnectEvent event) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
        log.info(">>> WebSocket CONNECT: sessionId={}, user={}", 
                accessor.getSessionId(),
                accessor.getUser() != null ? accessor.getUser().getName() : "anonymous");
    }

    @EventListener
    public void handleSessionConnected(SessionConnectedEvent event) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
        log.info(">>> WebSocket CONNECTED: sessionId={}, user={}", 
                accessor.getSessionId(),
                accessor.getUser() != null ? accessor.getUser().getName() : "anonymous");
    }

    @EventListener
    public void handleSessionDisconnect(SessionDisconnectEvent event) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
        log.info(">>> WebSocket DISCONNECT: sessionId={}, user={}", 
                accessor.getSessionId(),
                accessor.getUser() != null ? accessor.getUser().getName() : "anonymous");
    }

    @EventListener
    public void handleSessionSubscribe(SessionSubscribeEvent event) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
        log.info(">>> WebSocket SUBSCRIBE: sessionId={}, destination={}, user={}", 
                accessor.getSessionId(),
                accessor.getDestination(),
                accessor.getUser() != null ? accessor.getUser().getName() : "anonymous");
    }

    @EventListener
    public void handleSessionUnsubscribe(SessionUnsubscribeEvent event) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
        log.info(">>> WebSocket UNSUBSCRIBE: sessionId={}, user={}", 
                accessor.getSessionId(),
                accessor.getUser() != null ? accessor.getUser().getName() : "anonymous");
    }
}
