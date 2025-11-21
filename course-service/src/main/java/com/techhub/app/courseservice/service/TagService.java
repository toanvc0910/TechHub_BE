package com.techhub.app.courseservice.service;

import com.techhub.app.courseservice.dto.response.TagDTO;
import com.techhub.app.courseservice.entity.Tag;
import java.util.List;
import java.util.UUID;

public interface TagService {
    TagDTO createTag(TagDTO tagDTO);

    TagDTO getTag(UUID id);

    List<TagDTO> getAllTags();

    TagDTO updateTag(UUID id, TagDTO tagDTO);

    void deleteTag(UUID id);
}
