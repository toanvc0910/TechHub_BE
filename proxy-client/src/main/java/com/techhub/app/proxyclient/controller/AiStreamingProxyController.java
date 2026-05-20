package com.techhub.app.proxyclient.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;

import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Proxy controller for AI Chat streaming endpoints (SSE)
 * Feign doesn't support SSE, so we use WebClient to forward streaming requests
 */
@RestController
@RequestMapping("/api/proxy/ai/chat")
@RequiredArgsConstructor
@Slf4j
public class AiStreamingProxyController {

        private final WebClient.Builder webClientBuilder;

        // Use Eureka service name instead of hardcoded URL
        // This will be resolved via @LoadBalanced WebClient
        private static final String AI_SERVICE_NAME = "AI-SERVICE";
        private static final String AI_SERVICE_BASE_URL = "http://" + AI_SERVICE_NAME;

        /**
         * Proxy streaming chat request to AI-SERVICE
         * Forwards SSE stream from AI-SERVICE to client
         */
        @PostMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
        public Flux<ServerSentEvent<String>> streamChat(
                        @RequestBody Map<String, Object> request,
                        HttpServletRequest servletRequest) {
                log.info("🚀 [AiStreamingProxy] ===== STREAMING REQUEST STARTED =====");
                log.info("🚀 [AiStreamingProxy] Request body: {}", request);
                log.info("🚀 [AiStreamingProxy] Target URL: {}", AI_SERVICE_BASE_URL + "/api/ai/chat/stream");

                WebClient.RequestHeadersSpec<?> requestSpec = webClientBuilder.build()
                                .post()
                                .uri(AI_SERVICE_BASE_URL + "/api/ai/chat/stream")
                                .contentType(MediaType.APPLICATION_JSON)
                                .accept(MediaType.TEXT_EVENT_STREAM)
                                .bodyValue(request);
                applyTrustedHeaders(requestSpec, servletRequest);

                return requestSpec
                                .retrieve()
                                .bodyToFlux(new ParameterizedTypeReference<ServerSentEvent<String>>() {
                                })
                                .doOnNext(event -> log.info(
                                                "📦 [AiStreamingProxy] Event from AI-SERVICE: event={}, data={}",
                                                event.event(), event.data()))
                                .map(event -> ServerSentEvent.<String>builder()
                                                .event(event.event() == null ? "message" : event.event())
                                                .id(event.id())
                                                .data(event.data())
                                                .build())
                                .doOnNext(sse -> log.info(
                                                "📤 [AiStreamingProxy] Sending SSE to client: event={}, data={}",
                                                sse.event(), sse.data()))
                                .filter(sse -> sse.data() != null)
                                .doOnSubscribe(sub -> log.info("✅ [AiStreamingProxy] Client subscribed to stream"))
                                .doOnComplete(() -> log.info("✅ [AiStreamingProxy] ===== STREAM COMPLETED ====="))
                                .doOnError(error -> log.error("❌ [AiStreamingProxy] Stream error: {}",
                                                error.getMessage(), error))
                                .onErrorResume(error -> {
                                        log.error("❌ [AiStreamingProxy] Error resuming with error event: {}",
                                                        error.getMessage());
                                        return Flux.just(
                                                        ServerSentEvent.<String>builder()
                                                                        .event("error")
                                                                        .data("Streaming error: " + error.getMessage())
                                                                        .build());
                                });
        }

        /**
         * Simple streaming endpoint (GET request for quick queries)
         */
        @GetMapping(value = "/stream/simple", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
        public Flux<ServerSentEvent<String>> streamSimple(
                        @RequestParam String message,
                        @RequestParam UUID userId,
                        HttpServletRequest servletRequest) {
                log.info("🚀 [AiStreamingProxy] Proxying simple streaming for user: {}", userId);

                WebClient.RequestHeadersSpec<?> requestSpec = webClientBuilder.build()
                                .get()
                                .uri(uriBuilder -> uriBuilder
                                                .scheme("http")
                                                .host(AI_SERVICE_NAME)
                                                .path("/api/ai/chat/stream/simple")
                                                .queryParam("message", message)
                                                .queryParam("userId", userId)
                                                .build())
                                .accept(MediaType.TEXT_EVENT_STREAM);
                applyTrustedHeaders(requestSpec, servletRequest);

                return requestSpec
                                .retrieve()
                                .bodyToFlux(new ParameterizedTypeReference<ServerSentEvent<String>>() {
                                })
                                .map(event -> ServerSentEvent.<String>builder()
                                                .event(event.event() == null ? "message" : event.event())
                                                .id(event.id())
                                                .data(event.data())
                                                .build())
                                .filter(sse -> sse.data() != null && !sse.data().isEmpty())
                                .doOnComplete(() -> log.info("✅ [AiStreamingProxy] Simple stream completed"));
        }

        private void applyTrustedHeaders(
                        WebClient.RequestHeadersSpec<?> requestSpec,
                        HttpServletRequest servletRequest) {
                requestSpec.header("X-Request-Source", "proxy-client");

                Object userId = servletRequest.getAttribute("userId");
                if (userId != null) {
                        requestSpec.header("X-User-Id", userId.toString());
                }

                Object userEmail = servletRequest.getAttribute("userEmail");
                if (userEmail != null) {
                        requestSpec.header("X-User-Email", userEmail.toString());
                }

                Object userRoles = servletRequest.getAttribute("userRoles");
                if (userRoles instanceof List<?>) {
                        String roles = ((List<?>) userRoles).stream()
                                        .map(Object::toString)
                                        .collect(Collectors.joining(","));
                        requestSpec.header("X-User-Roles", roles);
                } else if (userRoles != null) {
                        requestSpec.header("X-User-Roles", userRoles.toString());
                }

                String userAgent = servletRequest.getHeader("User-Agent");
                if (userAgent != null) {
                        requestSpec.header("User-Agent", userAgent);
                }

                String xForwardedFor = servletRequest.getHeader("X-Forwarded-For");
                if (xForwardedFor != null) {
                        requestSpec.header("X-Forwarded-For", xForwardedFor);
                }
        }

        /**
         * Health check for streaming endpoint
         */
        @GetMapping(value = "/stream/health", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
        public Flux<ServerSentEvent<String>> streamHealth() {
                log.info("🏥 [AiStreamingProxy] Streaming health check");
                return Flux.just(
                                ServerSentEvent.<String>builder()
                                                .event("health")
                                                .data("OK")
                                                .build());
        }
}
