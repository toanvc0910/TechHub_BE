package com.techhub.app.proxyclient.dto;

import com.techhub.app.proxyclient.constant.SkillCategory;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SkillDTO {

    private UUID id;
    private String name;
    private String thumbnail;
    private SkillCategory category;
}