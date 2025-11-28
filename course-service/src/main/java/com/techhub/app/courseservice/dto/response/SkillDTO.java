package com.techhub.app.courseservice.dto.response;

import com.techhub.app.courseservice.enums.SkillCategory;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SkillDTO {
    private UUID id;
    private String name;
    private String thumbnail;
    private SkillCategory category;
}