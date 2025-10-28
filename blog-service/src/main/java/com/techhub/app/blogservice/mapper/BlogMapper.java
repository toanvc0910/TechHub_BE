package com.techhub.app.blogservice.mapper;

import com.techhub.app.blogservice.dto.BlogAttachmentDto;
import com.techhub.app.blogservice.dto.request.BlogRequest;
import com.techhub.app.blogservice.dto.response.BlogResponse;
import com.techhub.app.blogservice.entity.Blog;
import com.techhub.app.blogservice.enums.BlogStatus;
import com.techhub.app.blogservice.model.BlogAttachment;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Component
public class BlogMapper {

    public Blog toEntity(BlogRequest request) {
        Blog blog = new Blog();
        applyRequest(blog, request);
        blog.setStatus(request.getStatus() != null ? request.getStatus() : BlogStatus.DRAFT);
        return blog;
    }

    public void applyRequest(Blog blog, BlogRequest request) {
        blog.setTitle(request.getTitle().trim());
        blog.setContent(request.getContent().trim());
        blog.setThumbnail(request.getThumbnail() != null ? request.getThumbnail().trim() : null);
        blog.setTags(normalizeTags(request.getTags()));
        blog.setAttachments(toAttachmentEntities(request.getAttachments()));
        if (request.getStatus() != null) {
            blog.setStatus(request.getStatus());
        }
    }

    public BlogResponse toResponse(Blog blog) {
        return BlogResponse.builder()
                .id(blog.getId())
                .title(blog.getTitle())
                .content(blog.getContent())
                .thumbnail(blog.getThumbnail())
                .status(blog.getStatus())
                .tags(blog.getTags())
                .attachments(toAttachmentDtos(blog.getAttachments()))
                .authorId(blog.getAuthorId())
                .created(blog.getCreated())
                .updated(blog.getUpdated())
                .active(blog.getIsActive())
                .build();
    }

    private List<String> normalizeTags(List<String> tags) {
        if (tags == null) {
            return Collections.emptyList();
        }
        return tags.stream()
                .filter(Objects::nonNull)
                .map(String::trim)
                .map(String::toLowerCase)
                .filter(tag -> !tag.isEmpty())
                .distinct()
                .collect(Collectors.toList());
    }

    private List<BlogAttachment> toAttachmentEntities(List<BlogAttachmentDto> attachmentDtos) {
        if (attachmentDtos == null) {
            return Collections.emptyList();
        }
        return attachmentDtos.stream()
                .filter(Objects::nonNull)
                .map(dto -> BlogAttachment.builder()
                        .type(dto.getType())
                        .url(dto.getUrl())
                        .caption(dto.getCaption())
                        .altText(dto.getAltText())
                        .build())
                .collect(Collectors.toList());
    }

    private List<BlogAttachmentDto> toAttachmentDtos(List<BlogAttachment> attachments) {
        if (attachments == null) {
            return Collections.emptyList();
        }
        return attachments.stream()
                .filter(Objects::nonNull)
                .map(attachment -> BlogAttachmentDto.builder()
                        .type(attachment.getType())
                        .url(attachment.getUrl())
                        .caption(attachment.getCaption())
                        .altText(attachment.getAltText())
                        .build())
                .collect(Collectors.toList());
    }
}
