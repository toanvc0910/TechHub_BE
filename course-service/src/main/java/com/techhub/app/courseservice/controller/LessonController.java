package com.techhub.app.courseservice.controller;

import com.techhub.app.courseservice.dto.CreateLessonDTO;
import com.techhub.app.courseservice.dto.LessonDTO;
import com.techhub.app.courseservice.service.LessonService;
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
@RequestMapping("/lessons")
@Tag(name = "Lesson Management", description = "APIs for managing lessons")
public class LessonController {

    private final LessonService lessonService;

    public LessonController(LessonService lessonService) {
        this.lessonService = lessonService;
    }

    @PostMapping
    @Operation(summary = "Create a new lesson")
    public ResponseEntity<ResponseWrapper<LessonDTO>> createLesson(@Valid @RequestBody CreateLessonDTO createLessonDTO) {
        LessonDTO lessonDTO = lessonService.createLesson(createLessonDTO);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ResponseWrapper.success(lessonDTO, "Lesson created successfully"));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get lesson by ID")
    public ResponseEntity<ResponseWrapper<LessonDTO>> getLessonById(@PathVariable UUID id) {
        LessonDTO lessonDTO = lessonService.getLessonById(id);
        return ResponseEntity.ok(ResponseWrapper.success(lessonDTO));
    }

    @GetMapping
    @Operation(summary = "Get all lessons")
    public ResponseEntity<ResponseWrapper<List<LessonDTO>>> getAllLessons() {
        List<LessonDTO> lessons = lessonService.getAllLessons();
        return ResponseEntity.ok(ResponseWrapper.success(lessons));
    }

    @GetMapping("/chapter/{chapterId}")
    @Operation(summary = "Get lessons by chapter")
    public ResponseEntity<ResponseWrapper<List<LessonDTO>>> getLessonsByChapter(@PathVariable UUID chapterId) {
        List<LessonDTO> lessons = lessonService.getLessonsByChapter(chapterId);
        return ResponseEntity.ok(ResponseWrapper.success(lessons));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update lesson")
    public ResponseEntity<ResponseWrapper<LessonDTO>> updateLesson(
            @PathVariable UUID id,
            @RequestBody CreateLessonDTO updateLessonDTO) {
        LessonDTO lessonDTO = lessonService.updateLesson(id, updateLessonDTO);
        return ResponseEntity.ok(ResponseWrapper.success(lessonDTO, "Lesson updated successfully"));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete lesson")
    public ResponseEntity<ResponseWrapper<Void>> deleteLesson(@PathVariable UUID id) {
        lessonService.deleteLesson(id);
        return ResponseEntity.ok(ResponseWrapper.success(null, "Lesson deleted successfully"));
    }
}
