package com.techhub.app.aiservice.controller;

import com.techhub.app.aiservice.service.VectorIndexingService;
import com.techhub.app.aiservice.service.AiProviderConfigService;
import com.techhub.app.commonservice.payload.GlobalResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/ai/admin")
@RequiredArgsConstructor
@Slf4j
public class AdminAiController {

        private final VectorIndexingService vectorIndexingService;
        private final AiProviderConfigService aiProviderConfigService;

        /**
         * Batch index all existing courses into Qdrant
         * Use this when:
         * - AI Service starts for the first time
         * - Qdrant collection was deleted
         * - Need to rebuild embeddings
         */
        @PostMapping("/reindex-courses")
        public ResponseEntity<GlobalResponse<Map<String, Object>>> reindexAllCourses(
                        HttpServletRequest servletRequest) {
                log.info("🔄 Starting batch reindex of all courses...");

                int indexed = vectorIndexingService.reindexAllCourses();

                Map<String, Object> data = new HashMap<>();
                data.put("total_indexed", indexed);
                data.put("collection", "course_embeddings");

                return ResponseEntity.ok(
                                GlobalResponse.success(
                                                "Reindexed " + indexed + " courses successfully",
                                                data)
                                                .withStatus("REINDEX_COMPLETED")
                                                .withPath(servletRequest.getRequestURI()));
        }

        /**
         * Batch index all existing lessons into Qdrant
         * Use this when:
         * - New lesson types are introduced
         * - Qdrant collection for lessons was deleted
         * - Need to rebuild lesson embeddings
         */
        @PostMapping("/reindex-lessons")
        public ResponseEntity<GlobalResponse<Map<String, Object>>> reindexLessons(HttpServletRequest servletRequest) {
                log.info("🔄 Starting batch reindex of all lessons...");
                int count = vectorIndexingService.reindexAllLessons();

                Map<String, Object> data = new HashMap<>();
                data.put("total_indexed", count);
                data.put("collection", "course_embeddings"); // Reusing collection for now

                return ResponseEntity.ok(
                                GlobalResponse.success(
                                                "Reindexed " + count + " lessons successfully",
                                                data)
                                                .withStatus("REINDEX_COMPLETED")
                                                .withPath(servletRequest.getRequestURI()));
        }

        /**
         * Batch index everything: Courses, Lessons, Enrollments
         */
        @PostMapping("/reindex-all")
        public ResponseEntity<GlobalResponse<Map<String, Object>>> reindexAll(HttpServletRequest servletRequest) {
                log.info("🚀 Starting full system reindexing...");
                Map<String, Integer> counts = vectorIndexingService.reindexAll();

                Map<String, Object> data = new HashMap<>();
                data.put("counts", counts);

                return ResponseEntity.ok(
                                GlobalResponse.success(
                                                "Full system reindexing completed successfully",
                                                data)
                                                .withStatus("REINDEX_COMPLETED")
                                                .withPath(servletRequest.getRequestURI()));
        }

        /**
         * Get Qdrant collection statistics
         */
        @GetMapping("/qdrant-stats")
        public ResponseEntity<GlobalResponse<Map<String, Object>>> getQdrantStats(HttpServletRequest servletRequest) {
                Map<String, Object> stats = vectorIndexingService.getCollectionStats();

                return ResponseEntity.ok(
                                GlobalResponse.success("Qdrant statistics retrieved", stats)
                                                .withPath(servletRequest.getRequestURI()));
        }

        @GetMapping("/provider-config")
        public ResponseEntity<GlobalResponse<Map<String, Object>>> getProviderConfig(
                        HttpServletRequest servletRequest) {
                Map<String, Object> data = new HashMap<>();
                data.put("provider", aiProviderConfigService.getProvider());
                data.put("activeChatModel", aiProviderConfigService.getActiveChatModel());
                data.put("models", aiProviderConfigService.getCurrentModels());
                data.put("supportedProviders", java.util.List.of("openai", "gemini"));
                data.put("supportedChatModels", aiProviderConfigService.getSupportedChatModels());

                return ResponseEntity.ok(
                                GlobalResponse.success("AI provider configuration retrieved", data)
                                                .withPath(servletRequest.getRequestURI()));
        }

        @PostMapping("/provider-config")
        public ResponseEntity<GlobalResponse<Map<String, Object>>> updateProviderConfig(
                        @RequestBody Map<String, String> request,
                        HttpServletRequest servletRequest) {
                String provider = request == null ? null : request.get("provider");
                String targetProvider = provider;
                if (targetProvider != null && !aiProviderConfigService.isSupported(targetProvider)) {
                        return ResponseEntity.badRequest().body(
                                        GlobalResponse.<Map<String, Object>>error(
                                                        "Invalid provider. Supported values: openai, gemini")
                                                        .withPath(servletRequest.getRequestURI()));
                }

                if (targetProvider == null || targetProvider.isBlank()) {
                        targetProvider = aiProviderConfigService.getProvider();
                }

                if (provider != null && !provider.isBlank()) {
                        aiProviderConfigService.setProvider(provider);
                }

                String requestedChatModel = request == null ? null : request.get("chatModel");
                if (requestedChatModel != null && !requestedChatModel.isBlank()) {
                        if (!aiProviderConfigService.isModelSupported(targetProvider, requestedChatModel)) {
                                return ResponseEntity.badRequest().body(
                                                GlobalResponse.<Map<String, Object>>error(
                                                                "Invalid chatModel for provider '" + targetProvider
                                                                                + "'")
                                                                .withPath(servletRequest.getRequestURI()));
                        }
                        aiProviderConfigService.setModelForProvider(targetProvider, requestedChatModel);
                }

                String activeProvider = aiProviderConfigService.getProvider();
                Map<String, Object> data = new HashMap<>();
                data.put("provider", activeProvider);
                data.put("activeChatModel", aiProviderConfigService.getActiveChatModel());
                data.put("models", aiProviderConfigService.getCurrentModels());
                data.put("supportedChatModels", aiProviderConfigService.getSupportedChatModels());

                return ResponseEntity.ok(
                                GlobalResponse.success("AI provider updated successfully", data)
                                                .withStatus("PROVIDER_UPDATED")
                                                .withPath(servletRequest.getRequestURI()));
        }
}
