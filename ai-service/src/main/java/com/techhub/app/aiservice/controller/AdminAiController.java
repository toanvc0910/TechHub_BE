package com.techhub.app.aiservice.controller;

import com.techhub.app.aiservice.service.VectorIndexingService;
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

    /**
     * Batch index all existing courses into Qdrant
     * Use this when:
     * - AI Service starts for the first time
     * - Qdrant collection was deleted
     * - Need to rebuild embeddings
     */
    @PostMapping("/reindex-courses")
    public ResponseEntity<GlobalResponse<Map<String, Object>>> reindexAllCourses(HttpServletRequest servletRequest) {
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
     * Get Qdrant collection statistics
     */
    @GetMapping("/qdrant-stats")
    public ResponseEntity<GlobalResponse<Map<String, Object>>> getQdrantStats(HttpServletRequest servletRequest) {
        Map<String, Object> stats = vectorIndexingService.getCollectionStats();

        return ResponseEntity.ok(
                GlobalResponse.success("Qdrant statistics retrieved", stats)
                        .withPath(servletRequest.getRequestURI()));
    }
}
