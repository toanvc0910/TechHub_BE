package com.techhub.app.proxyclient.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;

import java.util.Map;
import java.util.UUID;

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
        public Flux<ServerSentEvent<String>> streamChat(@RequestBody Map<String, Object> request) {
                log.info("🚀 [AiStreamingProxy] ===== STREAMING REQUEST STARTED =====");
                log.info("🚀 [AiStreamingProxy] Request body: {}", request);
                log.info("🚀 [AiStreamingProxy] Target URL: {}", AI_SERVICE_BASE_URL + "/api/ai/chat/stream");

                return webClientBuilder.build()
                                .post()
                                .uri(AI_SERVICE_BASE_URL + "/api/ai/chat/stream")
                                .contentType(MediaType.APPLICATION_JSON)
                                .header("X-Request-Source", "proxy-client")
                                .bodyValue(request)
                                .retrieve()
                                .bodyToFlux(String.class)
                                .doOnNext(rawData -> log.info("📦 [AiStreamingProxy] Raw data from AI-SERVICE: {}",
                                                rawData))
                                .map(data -> {
                                        log.info("🔄 [AiStreamingProxy] Processing data: {}", data);
                                        // Parse SSE format from AI-SERVICE
                                        if (data.startsWith("event:")) {
                                                log.info("🎫 [AiStreamingProxy] Event line detected, skipping");
                                                // Handle event type - return null to filter out
                                                return ServerSentEvent.<String>builder()
                                                                .event("skip")
                                                                .data(null)
                                                                .build();
                                        } else if (data.startsWith("data:")) {
                                                // Extract JSON data after "data:"
                                                String content = data.substring(5);
                                                // Only trim for [DONE] check
                                                if (content.trim().equals("[DONE]")) {
                                                        log.info("🏁 [AiStreamingProxy] DONE signal received");
                                                        return ServerSentEvent.<String>builder()
                                                                        .event("done")
                                                                        .data("[DONE]")
                                                                        .build();
                                                }
                                                // Pass through JSON data as-is (contains {"content":"..."})
                                                log.info("📨 [AiStreamingProxy] Data content: '{}'", content);
                                                return ServerSentEvent.<String>builder()
                                                                .event("message")
                                                                .data(content)
                                                                .build();
                                        } else {
                                                // Raw data - pass through as-is
                                                log.info("📦 [AiStreamingProxy] Raw data (no prefix): {}", data);
                                                return ServerSentEvent.<String>builder()
                                                                .event("message")
                                                                .data(data)
                                                                .build();
                                        }
                                })
                                .doOnNext(sse -> log.info(
                                                "📤 [AiStreamingProxy] Sending SSE to client: event={}, data={}",
                                                sse.event(), sse.data()))
                                // Filter out null data and skip events, but KEEP spaces!
                                .filter(sse -> sse.data() != null && !"skip".equals(sse.event()))
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
                        @RequestParam UUID userId) {
                log.info("🚀 [AiStreamingProxy] Proxying simple streaming for user: {}", userId);

                return webClientBuilder.build()
                                .get()
                                .uri(AI_SERVICE_BASE_URL + "/api/ai/chat/stream/simple?message="
                                                + message + "&userId=" + userId)
                                .header("X-Request-Source", "proxy-client")
                                .retrieve()
                                .bodyToFlux(String.class)
                                .map(data -> {
                                        if (data.startsWith("data:")) {
                                                String content = data.substring(5).trim();
                                                if (content.equals("[DONE]")) {
                                                        return ServerSentEvent.<String>builder()
                                                                        .event("done")
                                                                        .data("[DONE]")
                                                                        .build();
                                                }
                                                return ServerSentEvent.<String>builder()
                                                                .event("message")
                                                                .data(content)
                                                                .build();
                                        }
                                        return ServerSentEvent.<String>builder()
                                                        .event("message")
                                                        .data(data)
                                                        .build();
                                })
                                .filter(sse -> sse.data() != null && !sse.data().isEmpty())
                                .doOnComplete(() -> log.info("✅ [AiStreamingProxy] Simple stream completed"));
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
