package com.techhub.app.courseservice.service.impl;

import com.techhub.app.commonservice.context.UserContext;
import com.techhub.app.commonservice.enums.UserRole;
import com.techhub.app.commonservice.exception.ForbiddenException;
import com.techhub.app.commonservice.exception.NotFoundException;
import com.techhub.app.commonservice.exception.UnauthorizedException;
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
        ensureInstructorOrAdmin();
        final Skill skill = new Skill();
        skill.setName(skillDTO.getName());
        skill.setCategory(skillDTO.getCategory());
        // set thumbnail if provided so created skill returns thumbnail URL
        skill.setThumbnail(skillDTO.getThumbnail());
        UUID currentUserId = requireCurrentUser();
        skill.setCreatedBy(currentUserId);
        skill.setUpdatedBy(currentUserId);
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
            UUID currentUserId = requireCurrentUser();
            ensureCanManage(skill.getCreatedBy(), currentUserId);
            skill.setName(skillDTO.getName());
            skill.setThumbnail(skillDTO.getThumbnail());
            skill.setCategory(skillDTO.getCategory());
            skill.setUpdatedBy(currentUserId);
            return toDTO(skillRepository.save(skill));
        }).orElse(null);
    }

    @Override
    public void deleteSkill(UUID id) {
        Skill skill = skillRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Skill not found"));
        ensureCanManage(skill.getCreatedBy(), requireCurrentUser());
        skillRepository.delete(skill);
    }

    private SkillDTO toDTO(final Skill skill) {
        final SkillDTO skillDTO = new SkillDTO();
        skillDTO.setId(skill.getId());
        skillDTO.setName(skill.getName());
        skillDTO.setCategory(skill.getCategory());
        skillDTO.setThumbnail(skill.getThumbnail());
        skillDTO.setCreatedBy(skill.getCreatedBy());
        return skillDTO;
    }

    private UUID requireCurrentUser() {
        UUID currentUserId = UserContext.getCurrentUserId();
        if (currentUserId == null) {
            throw new UnauthorizedException("Authentication required");
        }
        return currentUserId;
    }

    private void ensureCanManage(UUID creatorId, UUID currentUserId) {
        if (UserContext.hasAnyRole(UserRole.ADMIN.name(), UserRole.SUPER_ADMIN.name())) {
            return;
        }
        if (!UserContext.hasRole(UserRole.INSTRUCTOR.name()) || !currentUserId.equals(creatorId)) {
            throw new ForbiddenException("Instructors can only modify skills that they created");
        }
    }

    private void ensureInstructorOrAdmin() {
        if (!UserContext.hasAnyRole(UserRole.ADMIN.name(), UserRole.SUPER_ADMIN.name(), UserRole.INSTRUCTOR.name())) {
            throw new ForbiddenException("Only instructors or admins can create skills");
        }
    }
}
