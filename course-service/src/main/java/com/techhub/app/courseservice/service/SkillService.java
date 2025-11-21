package com.techhub.app.courseservice.service;

import com.techhub.app.courseservice.dto.response.SkillDTO;
import com.techhub.app.courseservice.entity.Skill;
import java.util.List;
import java.util.UUID;

public interface SkillService {
    SkillDTO createSkill(SkillDTO skillDTO);

    SkillDTO getSkill(UUID id);

    List<SkillDTO> getAllSkills();

    SkillDTO updateSkill(UUID id, SkillDTO skillDTO);

    void deleteSkill(UUID id);
}
