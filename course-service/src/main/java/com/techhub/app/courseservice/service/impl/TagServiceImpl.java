package com.techhub.app.courseservice.service.impl;

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
        Tag tag = new Tag();
        tag.setName(tagDTO.getName());
        // set audit timestamps required by DB not-null constraints
        OffsetDateTime now = OffsetDateTime.now();
        tag.setCreated(now);
        tag.setUpdated(now);
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
            tag.setName(tagDTO.getName());
            tag.setUpdated(OffsetDateTime.now());
            return toDTO(tagRepository.save(tag));
        }).orElse(null);
    }

    @Override
    public void deleteTag(UUID id) {
        tagRepository.deleteById(id);
    }

    private TagDTO toDTO(Tag tag) {
        return new TagDTO(tag.getId(), tag.getName());
    }
}
