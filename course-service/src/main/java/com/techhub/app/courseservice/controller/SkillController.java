package com.techhub.app.courseservice.controller;

import com.techhub.app.courseservice.dto.response.SkillDTO;
import com.techhub.app.courseservice.service.SkillService;
import com.techhub.app.commonservice.payload.GlobalResponse;
import com.techhub.app.commonservice.payload.PageGlobalResponse;
import javax.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/courses/skills")
@RequiredArgsConstructor
public class SkillController {
    private final SkillService skillService;

    @PostMapping
    public ResponseEntity<GlobalResponse<SkillDTO>> createSkill(@RequestBody SkillDTO skillDTO,
            HttpServletRequest request) {
        SkillDTO created = skillService.createSkill(skillDTO);
        return ResponseEntity.status(201)
                .body(GlobalResponse.success("Skill created successfully", created).withPath(request.getRequestURI()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<GlobalResponse<SkillDTO>> getSkill(@PathVariable UUID id, HttpServletRequest request) {
        SkillDTO skill = skillService.getSkill(id);
        return skill != null ? ResponseEntity.ok(GlobalResponse.success("Skill retrieved successfully", skill)
                .withPath(request.getRequestURI())) : ResponseEntity.notFound().build();
    }

    @GetMapping
    public ResponseEntity<PageGlobalResponse<SkillDTO>> getAllSkills(HttpServletRequest request) {
        List<SkillDTO> list = skillService.getAllSkills();

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

        return ResponseEntity.ok(PageGlobalResponse.success("Skills retrieved successfully", list, pagination)
                .withPath(request.getRequestURI()));
    }

    @PutMapping("/{id}")
    public ResponseEntity<GlobalResponse<SkillDTO>> updateSkill(@PathVariable UUID id, @RequestBody SkillDTO skillDTO,
            HttpServletRequest request) {
        SkillDTO updated = skillService.updateSkill(id, skillDTO);
        return updated != null ? ResponseEntity.ok(GlobalResponse.success("Skill updated successfully", updated)
                .withPath(request.getRequestURI())) : ResponseEntity.notFound().build();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<GlobalResponse<String>> deleteSkill(@PathVariable UUID id, HttpServletRequest request) {
        skillService.deleteSkill(id);
        return ResponseEntity.ok(GlobalResponse.success("Skill deleted successfully",
                "Skill with id " + id + " has been deleted").withPath(request.getRequestURI()));
    }
}
