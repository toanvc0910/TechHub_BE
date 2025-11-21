package com.techhub.app.courseservice.service.impl;

import com.techhub.app.courseservice.dto.response.SkillDTO;
import com.techhub.app.courseservice.entity.Skill;
import com.techhub.app.courseservice.repository.SkillRepository;
import com.techhub.app.courseservice.service.SkillService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SkillServiceImpl implements SkillService {
    private final SkillRepository skillRepository;

    @Override
    public SkillDTO createSkill(final SkillDTO skillDTO) {
        final Skill skill = new Skill();
        skill.setName(skillDTO.getName());
        skill.setCategory(skillDTO.getCategory());
        // set thumbnail if provided so created skill returns thumbnail URL
        skill.setThumbnail(skillDTO.getThumbnail());
        return toDTO(skillRepository.save(skill));
    }

    @Override
    public SkillDTO getSkill(UUID id) {
        return skillRepository.findById(id).map(this::toDTO).orElse(null);
    }

    @Override
    public List<SkillDTO> getAllSkills() {
        return skillRepository.findAll().stream().map(this::toDTO).collect(Collectors.toList());
    }

    @Override
    public SkillDTO updateSkill(UUID id, SkillDTO skillDTO) {
        return skillRepository.findById(id).map(skill -> {
            skill.setName(skillDTO.getName());
            skill.setThumbnail(skillDTO.getThumbnail());
            skill.setCategory(skillDTO.getCategory());
            return toDTO(skillRepository.save(skill));
        }).orElse(null);
    }

    @Override
    public void deleteSkill(UUID id) {
        skillRepository.deleteById(id);
    }

    private SkillDTO toDTO(final Skill skill) {
        final SkillDTO skillDTO = new SkillDTO();
        skillDTO.setId(skill.getId());
        skillDTO.setName(skill.getName());
        skillDTO.setCategory(skill.getCategory());
        skillDTO.setThumbnail(skill.getThumbnail());
        return skillDTO;
    }
}