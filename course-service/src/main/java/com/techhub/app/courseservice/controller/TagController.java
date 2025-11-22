package com.techhub.app.courseservice.controller;

import com.techhub.app.courseservice.dto.response.TagDTO;
import com.techhub.app.courseservice.service.TagService;
import com.techhub.app.commonservice.payload.GlobalResponse;
import com.techhub.app.commonservice.payload.PageGlobalResponse;
import javax.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/courses/tags")
@RequiredArgsConstructor
public class TagController {
    private final TagService tagService;

    @PostMapping
    public ResponseEntity<GlobalResponse<TagDTO>> createTag(@RequestBody TagDTO tagDTO, HttpServletRequest request) {
        TagDTO created = tagService.createTag(tagDTO);
        return ResponseEntity.status(201)
                .body(GlobalResponse.success("Tag created successfully", created).withPath(request.getRequestURI()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<GlobalResponse<TagDTO>> getTag(@PathVariable UUID id, HttpServletRequest request) {
        TagDTO tag = tagService.getTag(id);
        return tag != null ? ResponseEntity.ok(GlobalResponse.success("Tag retrieved successfully", tag)
                .withPath(request.getRequestURI())) : ResponseEntity.notFound().build();
    }

    @GetMapping
    public ResponseEntity<PageGlobalResponse<TagDTO>> getAllTags(HttpServletRequest request) {
        List<TagDTO> list = tagService.getAllTags();

        PageGlobalResponse.PaginationInfo pagination = PageGlobalResponse.PaginationInfo.builder()
                .page(0)
                .size(list.size())
                .totalElements(list.size())
                .totalPages(1)
                .first(true)
                .last(true)
                .hasNext(false)
                .hasPrevious(false)
                .build();

        return ResponseEntity.ok(PageGlobalResponse.success("Tags retrieved successfully", list, pagination)
                .withPath(request.getRequestURI()));
    }

    @PutMapping("/{id}")
    public ResponseEntity<GlobalResponse<TagDTO>> updateTag(@PathVariable UUID id, @RequestBody TagDTO tagDTO,
            HttpServletRequest request) {
        TagDTO updated = tagService.updateTag(id, tagDTO);
        return updated != null ? ResponseEntity.ok(GlobalResponse.success("Tag updated successfully", updated)
                .withPath(request.getRequestURI())) : ResponseEntity.notFound().build();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<GlobalResponse<String>> deleteTag(@PathVariable UUID id, HttpServletRequest request) {
        tagService.deleteTag(id);
        return ResponseEntity.ok(GlobalResponse.success("Tag deleted successfully",
                "Tag with id " + id + " has been deleted").withPath(request.getRequestURI()));
    }
}
