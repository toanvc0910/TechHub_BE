package com.techhub.app.aiservice.controller;

import com.techhub.app.aiservice.dto.request.ChatMessageRequest;
import com.techhub.app.aiservice.service.ChatStreamingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

import javax.validation.Valid;
import java.time.Duration;
import java.util.UUID;

@RestController
@RequestMapping("/api/ai/chat")
@Validated
@RequiredArgsConstructor
@Slf4j
public class ChatStreamingController {

        private final ChatStreamingService chatStreamingService;

        /**
         * Streaming chat endpoint using Server-Sent Events (SSE)
         * Client should use EventSource or fetch with ReadableStream to consume
         */
        @PostMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
        public Flux<ServerSentEvent<String>> streamMessage(@Valid @RequestBody ChatMessageRequest request) {
                log.info("🚀 [AI-SERVICE] ===== STREAM REQUEST RECEIVED =====");
                log.info("🚀 [AI-SERVICE] User: {}, Session: {}, Message: {}",
                                request.getUserId(), request.getSessionId(), request.getMessage());

                return chatStreamingService.sendStreamingMessage(request)
                                .doOnNext(chunk -> log.info("📦 [AI-SERVICE] Emitting chunk: {}", chunk))
                                .map(chunk -> {
                                        ServerSentEvent<String> sse = ServerSentEvent.<String>builder()
                                                        .event("message")
                                                        .data(chunk)
                                                        .build();
                                        log.info("📤 [AI-SERVICE] Built SSE: event={}, data={}", sse.event(),
                                                        sse.data());
                                        return sse;
                                })
                                .concatWith(Flux.defer(() -> {
                                        log.info("🏁 [AI-SERVICE] Appending DONE signal");
                                        return Flux.just(
                                                        ServerSentEvent.<String>builder()
                                                                        .event("done")
                                                                        .data("[DONE]")
                                                                        .build());
                                }))
                                .doOnSubscribe(sub -> log.info("✅ [AI-SERVICE] Client subscribed to stream"))
                                .doOnComplete(() -> log.info("✅ [AI-SERVICE] ===== STREAM COMPLETED ====="))
                                .doOnError(error -> log.error("❌ [AI-SERVICE] Stream error: {}", error.getMessage(),
                                                error));
        }

        /**
         * Simple streaming endpoint for quick queries without session
         */
        @GetMapping(value = "/stream/simple", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
        public Flux<ServerSentEvent<String>> streamSimple(
                        @RequestParam String message,
                        @RequestParam UUID userId) {
                log.info("🚀 Starting simple streaming for user: {}", userId);

                return chatStreamingService.streamSimpleResponse(message, userId)
                                .map(chunk -> ServerSentEvent.<String>builder()
                                                .event("message")
                                                .data(chunk)
                                                .build())
                                .concatWith(Flux.just(
                                                ServerSentEvent.<String>builder()
                                                                .event("done")
                                                                .data("[DONE]")
                                                                .build()))
                                .doOnComplete(() -> log.info("✅ Simple stream completed"));
        }

        /**
         * Health check for streaming endpoint
         */
        @GetMapping(value = "/stream/health", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
        public Flux<ServerSentEvent<String>> streamHealth() {
                return Flux.interval(Duration.ofSeconds(1))
                                .take(5)
                                .map(i -> ServerSentEvent.<String>builder()
                                                .event("ping")
                                                .data("pong-" + i)
                                                .build())
                                .concatWith(Flux.just(
                                                ServerSentEvent.<String>builder()
                                                                .event("done")
                                                                .data("[DONE]")
                                                                .build()));
        }
}
