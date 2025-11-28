package com.techhub.app.courseservice.mapper;

import com.techhub.app.courseservice.dto.request.ChapterRequest;
import com.techhub.app.courseservice.dto.request.CourseRequest;
import com.techhub.app.courseservice.dto.request.LessonAssetRequest;
import com.techhub.app.courseservice.dto.request.LessonRequest;
import com.techhub.app.courseservice.entity.Chapter;
import com.techhub.app.courseservice.entity.Course;
import com.techhub.app.courseservice.entity.Lesson;
import com.techhub.app.courseservice.entity.LessonAsset;
import com.techhub.app.courseservice.enums.CourseLevel;
import com.techhub.app.courseservice.enums.CourseStatus;
import com.techhub.app.courseservice.enums.Language;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class CourseMapper {

    public Course toEntity(CourseRequest request, UUID instructorId, UUID actorId) {
        Course course = new Course();
        course.setTitle(normalizeTitle(request.getTitle()));
        course.setDescription(normalizeText(request.getDescription()));
        course.setPrice(normalizePrice(request.getPrice()));
        course.setInstructorId(instructorId);
        course.setStatus(request.getStatus() != null ? request.getStatus() : CourseStatus.DRAFT);
        course.setLevel(request.getLevel() != null ? request.getLevel() : CourseLevel.ALL_LEVELS);
        course.setLanguage(request.getLanguage() != null ? request.getLanguage() : Language.VI);
        // TODO: Map skills/tags from request to CourseSkill/CourseTag entities here if
        // needed
        course.setDiscountPrice(normalizePrice(request.getDiscountPrice()));
        course.setPromoEndDate(request.getPromoEndDate());
        course.setThumbnail(normalizeText(request.getThumbnail()));
        course.setIntroVideoFile(normalizeText(request.getIntroVideo()));
        course.setObjectives(normalizeList(request.getObjectives(), false));
        course.setRequirements(normalizeList(request.getRequirements(), false));
        course.setCreatedBy(actorId);
        course.setUpdatedBy(actorId);
        return course;
    }

    public void updateEntity(Course course, CourseRequest request, UUID actorId) {
        if (request.getTitle() != null) {
            course.setTitle(normalizeTitle(request.getTitle()));
        }
        if (request.getDescription() != null) {
            course.setDescription(normalizeText(request.getDescription()));
        }
        if (request.getPrice() != null) {
            course.setPrice(normalizePrice(request.getPrice()));
        }
        if (request.getStatus() != null) {
            course.setStatus(request.getStatus());
        }
        if (request.getLevel() != null) {
            course.setLevel(request.getLevel());
        }
        if (request.getLanguage() != null) {
            course.setLanguage(request.getLanguage());
        }
        // TODO: Update skills/tags mapping here if needed
        if (request.getDiscountPrice() != null || request.getDiscountPrice() == null) {
            course.setDiscountPrice(normalizePrice(request.getDiscountPrice()));
        }
        course.setPromoEndDate(request.getPromoEndDate());
        if (request.getThumbnail() != null) {
            course.setThumbnail(normalizeText(request.getThumbnail()));
        }
        if (request.getIntroVideo() != null) {
            course.setIntroVideoFile(normalizeText(request.getIntroVideo()));
        }
        if (request.getObjectives() != null) {
            course.setObjectives(normalizeList(request.getObjectives(), false));
        }
        if (request.getRequirements() != null) {
            course.setRequirements(normalizeList(request.getRequirements(), false));
        }
        course.setUpdatedBy(actorId);
        course.setUpdated(OffsetDateTime.now());
    }

    public Chapter toChapterEntity(ChapterRequest request, Course course, UUID actorId, Integer orderIndex) {
        Chapter chapter = new Chapter();
        chapter.setTitle(normalizeTitle(request.getTitle()));
        chapter.setOrderIndex(orderIndex != null ? orderIndex : 1);
        chapter.setCourse(course);
        if (request.getMinCompletionThreshold() != null) {
            chapter.setMinCompletionThreshold(request.getMinCompletionThreshold());
        }
        if (request.getAutoUnlock() != null) {
            chapter.setAutoUnlock(request.getAutoUnlock());
        }
        if (request.getLocked() != null) {
            chapter.setLocked(request.getLocked());
        }
        chapter.setCreatedBy(actorId);
        chapter.setUpdatedBy(actorId);
        return chapter;
    }

    public void updateChapter(Chapter chapter, ChapterRequest request, UUID actorId) {
        if (request.getTitle() != null) {
            chapter.setTitle(normalizeTitle(request.getTitle()));
        }
        if (request.getOrderIndex() != null) {
            chapter.setOrderIndex(request.getOrderIndex());
        }
        if (request.getMinCompletionThreshold() != null) {
            chapter.setMinCompletionThreshold(request.getMinCompletionThreshold());
        }
        if (request.getAutoUnlock() != null) {
            chapter.setAutoUnlock(request.getAutoUnlock());
        }
        if (request.getLocked() != null) {
            chapter.setLocked(request.getLocked());
        }
        chapter.setUpdatedBy(actorId);
        chapter.setUpdated(OffsetDateTime.now());
    }

    public Lesson toLessonEntity(LessonRequest request, Chapter chapter, UUID actorId, Integer orderIndex) {
        log.info("🔵 CREATE LESSON - Request received:");
        log.info("  - title: {}", request.getTitle());
        log.info("  - description: {}", request.getDescription());
        log.info("  - content: {}", request.getContent());
        log.info("  - isFree: {}", request.getIsFree());
        log.info("  - contentType: {}", request.getContentType());
        log.info("  - videoUrl: {}", request.getVideoUrl());
        log.info("  - estimatedDuration: {}", request.getEstimatedDuration());
        log.info("  - orderIndex: {}", request.getOrderIndex());

        Lesson lesson = new Lesson();
        lesson.setTitle(normalizeTitle(request.getTitle()));
        lesson.setDescription(normalizeText(request.getDescription()));
        lesson.setOrderIndex(orderIndex != null ? orderIndex : 1);
        lesson.setChapter(chapter);
        lesson.setContentType(request.getContentType());
        lesson.setContent(normalizeText(request.getContent()));
        lesson.setIsFree(request.getIsFree() != null ? request.getIsFree() : false);

        log.info("🟢 CREATE LESSON - Entity created with:");
        log.info("  - description set to: {}", lesson.getDescription());
        log.info("  - content set to: {}", lesson.getContent());
        log.info("  - isFree set to: {}", lesson.getIsFree());
        if (request.getMandatory() != null) {
            lesson.setMandatory(request.getMandatory());
        }
        if (request.getCompletionWeight() != null) {
            lesson.setCompletionWeight(request.getCompletionWeight());
        }
        lesson.setEstimatedDuration(request.getEstimatedDuration());
        if (request.getWorkspaceEnabled() != null) {
            lesson.setWorkspaceEnabled(request.getWorkspaceEnabled());
        }
        lesson.setWorkspaceLanguages(normalizeList(request.getWorkspaceLanguages(), false));
        lesson.setWorkspaceTemplate(normalizeMap(request.getWorkspaceTemplate()));
        lesson.setVideoUrl(normalizeText(request.getVideoUrl()));
        lesson.setDocumentUrls(normalizeList(request.getDocumentUrls(), false));
        lesson.setCreatedBy(actorId);
        lesson.setUpdatedBy(actorId);
        return lesson;
    }

    public void updateLesson(Lesson lesson, LessonRequest request, UUID actorId) {
        log.info("🔵 UPDATE LESSON - Request received:");
        log.info("  - title: {}", request.getTitle());
        log.info("  - description: {}", request.getDescription());
        log.info("  - content: {}", request.getContent());
        log.info("  - isFree: {}", request.getIsFree());
        log.info("  - contentType: {}", request.getContentType());
        log.info("  - videoUrl: {}", request.getVideoUrl());
        log.info("  - estimatedDuration: {}", request.getEstimatedDuration());
        log.info("  - orderIndex: {}", request.getOrderIndex());

        if (request.getTitle() != null) {
            lesson.setTitle(normalizeTitle(request.getTitle()));
        }
        if (request.getDescription() != null) {
            lesson.setDescription(normalizeText(request.getDescription()));
            log.info("  ✅ Updated description to: {}", lesson.getDescription());
        }
        if (request.getOrderIndex() != null) {
            lesson.setOrderIndex(request.getOrderIndex());
        }
        if (request.getContentType() != null) {
            lesson.setContentType(request.getContentType());
        }
        if (request.getContent() != null) {
            lesson.setContent(normalizeText(request.getContent()));
            log.info("  ✅ Updated content to: {}", lesson.getContent());
        }
        if (request.getIsFree() != null) {
            lesson.setIsFree(request.getIsFree());
            log.info("  ✅ Updated isFree to: {}", lesson.getIsFree());
        }
        if (request.getMandatory() != null) {
            lesson.setMandatory(request.getMandatory());
        }
        if (request.getCompletionWeight() != null) {
            lesson.setCompletionWeight(request.getCompletionWeight());
        }
        if (request.getEstimatedDuration() != null) {
            lesson.setEstimatedDuration(request.getEstimatedDuration());
        }
        if (request.getWorkspaceEnabled() != null) {
            lesson.setWorkspaceEnabled(request.getWorkspaceEnabled());
        }
        if (request.getWorkspaceLanguages() != null) {
            lesson.setWorkspaceLanguages(normalizeList(request.getWorkspaceLanguages(), false));
        }
        if (request.getWorkspaceTemplate() != null) {
            lesson.setWorkspaceTemplate(normalizeMap(request.getWorkspaceTemplate()));
        }
        if (request.getVideoUrl() != null) {
            lesson.setVideoUrl(normalizeText(request.getVideoUrl()));
        }
        if (request.getDocumentUrls() != null) {
            lesson.setDocumentUrls(normalizeList(request.getDocumentUrls(), false));
        }
        lesson.setUpdatedBy(actorId);
        lesson.setUpdated(OffsetDateTime.now());
    }

    public LessonAsset toLessonAssetEntity(LessonAssetRequest request, Lesson lesson, UUID actorId,
            Integer orderIndex) {
        LessonAsset asset = new LessonAsset();
        asset.setLesson(lesson);
        asset.setAssetType(request.getAssetType());
        asset.setOrderIndex(orderIndex != null ? orderIndex : 0);
        asset.setTitle(normalizeText(request.getTitle()));
        asset.setDescription(normalizeText(request.getDescription()));
        asset.setFileId(request.getFileId());
        asset.setExternalUrl(normalizeText(request.getExternalUrl()));
        asset.setMetadata(normalizeMap(request.getMetadata()));
        asset.setCreatedBy(actorId);
        asset.setUpdatedBy(actorId);
        return asset;
    }

    public void updateLessonAsset(LessonAsset asset, LessonAssetRequest request, UUID actorId) {
        if (request.getAssetType() != null) {
            asset.setAssetType(request.getAssetType());
        }
        if (request.getOrderIndex() != null) {
            asset.setOrderIndex(request.getOrderIndex());
        }
        if (request.getTitle() != null) {
            asset.setTitle(normalizeText(request.getTitle()));
        }
        if (request.getDescription() != null) {
            asset.setDescription(normalizeText(request.getDescription()));
        }
        if (request.getFileId() != null || request.getFileId() == null) {
            asset.setFileId(request.getFileId());
        }
        if (request.getExternalUrl() != null || request.getExternalUrl() == null) {
            asset.setExternalUrl(normalizeText(request.getExternalUrl()));
        }
        if (request.getMetadata() != null) {
            asset.setMetadata(normalizeMap(request.getMetadata()));
        }
        asset.setUpdatedBy(actorId);
        asset.setUpdated(OffsetDateTime.now());
    }

    private String normalizeTitle(String value) {
        return value == null ? null : value.trim();
    }

    private String normalizeText(String value) {
        return value == null ? null : value.trim();
    }

    private BigDecimal normalizePrice(BigDecimal price) {
        if (price == null) {
            return null;
        }
        return price.compareTo(BigDecimal.ZERO) < 0 ? BigDecimal.ZERO : price;
    }

    private List<String> normalizeList(List<String> values, boolean toLowercase) {
        if (values == null) {
            return new ArrayList<>();
        }
        return values.stream()
                .filter(v -> v != null && !v.trim().isEmpty())
                .map(String::trim)
                .map(v -> toLowercase ? v.toLowerCase() : v)
                .distinct()
                .collect(Collectors.toList());
    }

    private Map<String, Object> normalizeMap(Map<String, Object> source) {
        return source == null ? Map.of() : source;
    }
}
