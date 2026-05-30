package com.techhub.app.courseservice.service.impl;

import com.techhub.app.commonservice.context.UserContext;
import com.techhub.app.commonservice.enums.UserRole;
import com.techhub.app.commonservice.exception.ForbiddenException;
import com.techhub.app.commonservice.exception.NotFoundException;
import com.techhub.app.commonservice.exception.UnauthorizedException;
import com.techhub.app.courseservice.dto.response.TagDTO;
import com.techhub.app.courseservice.entity.Tag;
import com.techhub.app.courseservice.repository.TagRepository;
import com.techhub.app.courseservice.service.TagService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TagServiceImpl implements TagService {
    private final TagRepository tagRepository;

    @Override
    public TagDTO createTag(TagDTO tagDTO) {
        ensureInstructorOrAdmin();
        Tag tag = new Tag();
        tag.setName(tagDTO.getName());
        // set audit timestamps required by DB not-null constraints
        OffsetDateTime now = OffsetDateTime.now();
        tag.setCreated(now);
        tag.setUpdated(now);
        UUID currentUserId = requireCurrentUser();
        tag.setCreatedBy(currentUserId);
        tag.setUpdatedBy(currentUserId);
        Tag saved = tagRepository.save(tag);
        return toDTO(saved);
    }

    @Override
    public TagDTO getTag(UUID id) {
        return tagRepository.findById(id).map(this::toDTO).orElse(null);
    }

    @Override
    public List<TagDTO> getAllTags() {
        return tagRepository.findAll().stream().map(this::toDTO).collect(Collectors.toList());
    }

    @Override
    public TagDTO updateTag(UUID id, TagDTO tagDTO) {
        return tagRepository.findById(id).map(tag -> {
            UUID currentUserId = requireCurrentUser();
            ensureCanManage(tag.getCreatedBy(), currentUserId);
            tag.setName(tagDTO.getName());
            tag.setUpdated(OffsetDateTime.now());
            tag.setUpdatedBy(currentUserId);
            return toDTO(tagRepository.save(tag));
        }).orElse(null);
    }

    @Override
    public void deleteTag(UUID id) {
        Tag tag = tagRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Tag not found"));
        ensureCanManage(tag.getCreatedBy(), requireCurrentUser());
        tagRepository.delete(tag);
    }

    private TagDTO toDTO(Tag tag) {
        return new TagDTO(tag.getId(), tag.getName(), tag.getCreatedBy());
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
            throw new ForbiddenException("Instructors can only modify tags that they created");
        }
    }

    private void ensureInstructorOrAdmin() {
        if (!UserContext.hasAnyRole(UserRole.ADMIN.name(), UserRole.SUPER_ADMIN.name(), UserRole.INSTRUCTOR.name())) {
            throw new ForbiddenException("Only instructors or admins can create tags");
        }
    }
}
