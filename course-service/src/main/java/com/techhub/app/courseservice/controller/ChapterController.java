package com.techhub.app.courseservice.controller;

import com.techhub.app.courseservice.dto.ChapterDTO;
import com.techhub.app.courseservice.dto.CreateChapterDTO;
import com.techhub.app.courseservice.service.ChapterService;
import com.techhub.app.courseservice.utils.ResponseWrapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import javax.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/chapters")
@Tag(name = "Chapter Management", description = "APIs for managing course chapters")
public class ChapterController {

    private final ChapterService chapterService;

    public ChapterController(ChapterService chapterService) {
        this.chapterService = chapterService;
    }

    @PostMapping
    @Operation(summary = "Create a new chapter")
    public ResponseEntity<ResponseWrapper<ChapterDTO>> createChapter(@Valid @RequestBody CreateChapterDTO createChapterDTO) {
        ChapterDTO chapterDTO = chapterService.createChapter(createChapterDTO);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ResponseWrapper.success(chapterDTO, "Chapter created successfully"));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get chapter by ID")
    public ResponseEntity<ResponseWrapper<ChapterDTO>> getChapterById(@PathVariable UUID id) {
        ChapterDTO chapterDTO = chapterService.getChapterById(id);
        return ResponseEntity.ok(ResponseWrapper.success(chapterDTO));
    }

    @GetMapping
    @Operation(summary = "Get all chapters")
    public ResponseEntity<ResponseWrapper<List<ChapterDTO>>> getAllChapters() {
        List<ChapterDTO> chapters = chapterService.getAllChapters();
        return ResponseEntity.ok(ResponseWrapper.success(chapters));
    }

    @GetMapping("/course/{courseId}")
    @Operation(summary = "Get chapters by course")
    public ResponseEntity<ResponseWrapper<List<ChapterDTO>>> getChaptersByCourse(@PathVariable UUID courseId) {
        List<ChapterDTO> chapters = chapterService.getChaptersByCourse(courseId);
        return ResponseEntity.ok(ResponseWrapper.success(chapters));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update chapter")
    public ResponseEntity<ResponseWrapper<ChapterDTO>> updateChapter(
            @PathVariable UUID id,
            @RequestBody CreateChapterDTO updateChapterDTO) {
        ChapterDTO chapterDTO = chapterService.updateChapter(id, updateChapterDTO);
        return ResponseEntity.ok(ResponseWrapper.success(chapterDTO, "Chapter updated successfully"));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete chapter")
    public ResponseEntity<ResponseWrapper<Void>> deleteChapter(@PathVariable UUID id) {
        chapterService.deleteChapter(id);
        return ResponseEntity.ok(ResponseWrapper.success(null, "Chapter deleted successfully"));
    }
}
