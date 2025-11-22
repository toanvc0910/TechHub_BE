package com.techhub.app.courseservice.service.impl;

import com.techhub.app.commonservice.context.UserContext;
import com.techhub.app.commonservice.exception.BadRequestException;
import com.techhub.app.commonservice.exception.ForbiddenException;
import com.techhub.app.commonservice.exception.NotFoundException;
import com.techhub.app.commonservice.exception.UnauthorizedException;
import com.techhub.app.courseservice.dto.request.ChapterRequest;
import com.techhub.app.courseservice.dto.request.CourseRequest;
import com.techhub.app.courseservice.dto.request.LessonAssetRequest;
import com.techhub.app.courseservice.dto.request.LessonRequest;
import com.techhub.app.courseservice.dto.response.ChapterResponse;
import com.techhub.app.courseservice.dto.response.CourseDetailResponse;
import com.techhub.app.courseservice.dto.response.CourseFileResource;
import com.techhub.app.courseservice.dto.response.CourseSummaryResponse;
import com.techhub.app.courseservice.dto.response.LessonAssetResponse;
import com.techhub.app.courseservice.dto.response.LessonResponse;
import com.techhub.app.courseservice.dto.response.SkillDTO;
import com.techhub.app.courseservice.dto.response.TagDTO;
import com.techhub.app.courseservice.entity.Chapter;
import com.techhub.app.courseservice.entity.Course;
import com.techhub.app.courseservice.entity.CourseSkill;
import com.techhub.app.courseservice.entity.CourseTag;
import com.techhub.app.courseservice.entity.Enrollment;
import com.techhub.app.courseservice.entity.Lesson;
import com.techhub.app.courseservice.entity.LessonAsset;
import com.techhub.app.courseservice.entity.Progress;
import com.techhub.app.courseservice.event.CourseEvent;
import com.techhub.app.courseservice.event.EventPublisher;
import com.techhub.app.courseservice.entity.Skill;
import com.techhub.app.courseservice.entity.Tag;
import com.techhub.app.courseservice.enums.CourseStatus;
import com.techhub.app.courseservice.enums.EnrollmentStatus;
import com.techhub.app.courseservice.enums.LessonAssetType;
import com.techhub.app.courseservice.enums.RatingTarget;
import com.techhub.app.courseservice.mapper.CourseMapper;
import com.techhub.app.courseservice.repository.ChapterRepository;
import com.techhub.app.courseservice.repository.CourseRepository;
import com.techhub.app.courseservice.repository.EnrollmentRepository;
import com.techhub.app.courseservice.repository.LessonAssetRepository;
import com.techhub.app.courseservice.repository.LessonRepository;
import com.techhub.app.courseservice.repository.ProgressRepository;
import com.techhub.app.courseservice.repository.RatingRepository;
import com.techhub.app.courseservice.repository.SkillRepository;
import com.techhub.app.courseservice.repository.TagRepository;
import com.techhub.app.courseservice.service.CourseService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class CourseServiceImpl implements CourseService {

    private static final String ROLE_ADMIN = "ADMIN";
    private static final String ROLE_INSTRUCTOR = "INSTRUCTOR";

    private final CourseRepository courseRepository;
    private final ChapterRepository chapterRepository;
    private final LessonRepository lessonRepository;
    private final LessonAssetRepository lessonAssetRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final CourseMapper courseMapper;
    private final ProgressRepository progressRepository;
    private final RatingRepository ratingRepository;
    private final EventPublisher eventPublisher;
    private final SkillRepository skillRepository;
    private final TagRepository tagRepository;

    @Override
    @Transactional(readOnly = true)
    public Page<CourseSummaryResponse> getCourses(String search, Pageable pageable) {
        String normalized = normalizeSearch(search);
        UUID currentUserId = UserContext.getCurrentUserId();
        boolean isAdmin = UserContext.hasAnyRole(ROLE_ADMIN);
        boolean isInstructor = UserContext.hasAnyRole(ROLE_INSTRUCTOR);

        Page<Course> courses;
        if (isAdmin) {
            courses = courseRepository.searchCourses(null, normalized, pageable);
        } else if (isInstructor && currentUserId != null) {
            courses = courseRepository.searchInstructorCourses(currentUserId, normalized, pageable);
        } else {
            courses = courseRepository.searchCourses(CourseStatus.PUBLISHED.name(), normalized, pageable);
        }
        return courses.map(this::buildCourseSummary);
    }

    @Override
    @Transactional(readOnly = true)
    public CourseDetailResponse getCourse(UUID courseId) {
        Course course = getActiveCourse(courseId);
        UUID currentUserId = UserContext.getCurrentUserId();
        boolean isManager = canManageCourse(course, currentUserId);
        boolean published = course.getStatus() == CourseStatus.PUBLISHED;

        if (!published && !isManager) {
            throw new NotFoundException("Course not found");
        }

        List<Progress> userProgress = currentUserId != null
                ? progressRepository.findByUserAndCourse(currentUserId, courseId)
                : Collections.emptyList();

        CourseProgressSnapshot snapshot = buildChapterSnapshot(course, currentUserId, isManager, userProgress);
        CourseSummaryResponse summary = buildCourseSummary(course);
        EnrollmentStatus enrollmentStatus = null;
        boolean enrolled = false;
        if (currentUserId != null) {
            Enrollment enrollment = enrollmentRepository.findByUserIdAndCourse_Id(currentUserId, courseId).orElse(null);
            if (enrollment != null) {
                enrollmentStatus = enrollment.getStatus();
                enrolled = Boolean.TRUE.equals(enrollment.getIsActive());
            }
        }

        return CourseDetailResponse.builder()
                .summary(summary)
                .chapters(snapshot.getChapters())
                .enrollmentStatus(enrollmentStatus)
                .enrolled(enrolled)
                .totalChapters(snapshot.getChapters().size())
                .totalLessons(snapshot.getTotalLessons())
                .totalEstimatedDurationMinutes(snapshot.getTotalEstimatedDurationMinutes())
                .overallProgress(snapshot.getOverallProgress())
                .currentChapterId(snapshot.getCurrentChapterId())
                .unlockedChapterIds(snapshot.getUnlockedChapterIds())
                .lockedChapterIds(snapshot.getLockedChapterIds())
                .completedLessons(snapshot.getCompletedLessons())
                .build();
    }

    @Override
    public CourseDetailResponse createCourse(CourseRequest request) {
        log.info("========== CourseServiceImpl.createCourse START ==========");
        ensureInstructorOrAdmin();
        UUID currentUserId = requireCurrentUser();
        UUID instructorId = resolveInstructorId(request.getInstructorId(), currentUserId);
        validateDiscount(request.getPrice(), request.getDiscountPrice());

        log.info("CourseServiceImpl.createCourse - Request received:");
        log.info("  - Title: {}", request.getTitle());
        log.info("  - Categories: {}", request.getCategories());
        log.info("  - Categories is null: {}", request.getCategories() == null);
        if (request.getCategories() != null) {
            log.info("  - Categories size: {}", request.getCategories().size());
            for (int i = 0; i < request.getCategories().size(); i++) {
                log.info("  - Category[{}]: '{}'", i, request.getCategories().get(i));
            }
        }
        log.info("  - Tags: {}", request.getTags());
        log.info("  - Tags is null: {}", request.getTags() == null);
        if (request.getTags() != null) {
            log.info("  - Tags size: {}", request.getTags().size());
        }

        Course course = courseMapper.toEntity(request, instructorId, currentUserId);
        log.info("CourseServiceImpl.createCourse - Course entity created, initial skills count: {}",
                course.getCourseSkills().size());

        courseRepository.save(course);
        log.info("CourseServiceImpl.createCourse - Course saved with ID: {}", course.getId());
        log.info("CourseServiceImpl.createCourse - After first save, skills count: {}",
                course.getCourseSkills().size());

        // Map skills and tags from request
        // Always call mapSkillsToCourse/mapTagsToCourse to handle null/empty cases
        // properly
        log.info(
                "CourseServiceImpl.createCourse - Calling mapSkillsToCourse (prefer request.skills, fallback to categories)...");
        mapSkillsToCourse(course, chooseSkills(request));
        log.info("CourseServiceImpl.createCourse - After mapSkillsToCourse, skills count: {}",
                course.getCourseSkills().size());

        log.info("CourseServiceImpl.createCourse - Calling mapTagsToCourse...");
        mapTagsToCourse(course, request.getTags());
        log.info("CourseServiceImpl.createCourse - After mapTagsToCourse, tags count: {}",
                course.getCourseTags().size());

        log.info("CourseServiceImpl.createCourse - Saving course with skills and tags...");
        courseRepository.save(course);
        log.info("CourseServiceImpl.createCourse - Course saved. Final skills count: {}, tags count: {}",
                course.getCourseSkills().size(),
                course.getCourseTags().size());

        // Verify skills were saved
        Course savedCourse = courseRepository.findById(course.getId()).orElse(null);
        if (savedCourse != null) {
            log.info("CourseServiceImpl.createCourse - Reloaded course from DB, skills count: {}, tags count: {}",
                    savedCourse.getCourseSkills().size(),
                    savedCourse.getCourseTags().size());
        }

        log.info("Course {} created by {} with {} skills and {} tags",
                course.getId(), currentUserId,
                course.getCourseSkills().size(),
                course.getCourseTags().size());
        log.info("========== CourseServiceImpl.createCourse END ==========");

        // Publish event for AI indexing
        publishCourseCreatedEvent(course);

        return getCourse(course.getId());
    }

    @Override
    public CourseDetailResponse updateCourse(UUID courseId, CourseRequest request) {
        log.info("========== CourseServiceImpl.updateCourse START ==========");
        log.info("updateCourse - Course ID: {}", courseId);
        log.info("updateCourse - Request skills: {}", request.getSkills());
        log.info("updateCourse - Request categories: {}", request.getCategories());
        log.info("updateCourse - Request tags: {}", request.getTags());

        Course course = getActiveCourse(courseId);
        UUID currentUserId = requireCurrentUser();
        if (!canManageCourse(course, currentUserId)) {
            throw new ForbiddenException("You are not allowed to update this course");
        }

        if (request.getInstructorId() != null && !request.getInstructorId().equals(course.getInstructorId())) {
            if (!UserContext.hasAnyRole(ROLE_ADMIN)) {
                throw new ForbiddenException("Only admins can reassign course instructors");
            }
            course.setInstructorId(request.getInstructorId());
        }

        validateDiscount(request.getPrice() != null ? request.getPrice() : course.getPrice(),
                request.getDiscountPrice());
        courseMapper.updateEntity(course, request, currentUserId);

        log.info("updateCourse - Before mapSkillsToCourse, course skills count: {}", course.getCourseSkills().size());

        // Update skills and tags if provided (prefer request.skills over categories)
        if (request.getSkills() != null || request.getCategories() != null) {
            mapSkillsToCourse(course, chooseSkills(request));
        }

        log.info("updateCourse - After mapSkillsToCourse, course skills count: {}", course.getCourseSkills().size());

        if (request.getTags() != null) {
            mapTagsToCourse(course, request.getTags());
        }

        log.info("updateCourse - After mapTagsToCourse, course tags count: {}", course.getCourseTags().size());

        courseRepository.save(course);
        log.info("Course {} updated by {} with {} skills and {} tags",
                courseId, currentUserId,
                course.getCourseSkills().size(),
                course.getCourseTags().size());
        log.info("========== CourseServiceImpl.updateCourse END ==========");

        // Publish event for AI re-indexing
        publishCourseUpdatedEvent(course);

        return getCourse(courseId);
    }

    @Override
    public void deleteCourse(UUID courseId) {
        Course course = getActiveCourse(courseId);
        UUID currentUserId = requireCurrentUser();
        if (!canManageCourse(course, currentUserId)) {
            throw new ForbiddenException("You are not allowed to delete this course");
        }
        course.setIsActive(false);
        course.setUpdatedBy(currentUserId);
        course.setUpdated(OffsetDateTime.now());
        courseRepository.save(course);
        log.info("Course {} soft-deleted by {}", courseId, currentUserId);

        // Publish event for AI de-indexing
        publishCourseDeletedEvent(course);
    }

    @Override
    public void enrollCourse(UUID courseId) {
        UUID currentUserId = requireCurrentUser();
        Course course = getActiveCourse(courseId);

        if (course.getStatus() != CourseStatus.PUBLISHED) {
            throw new BadRequestException("Course is not open for enrollment");
        }

        Enrollment enrollment = enrollmentRepository.findByUserIdAndCourse_Id(currentUserId, courseId)
                .orElseGet(() -> {
                    Enrollment entity = new Enrollment();
                    entity.setCourse(course);
                    entity.setUserId(currentUserId);
                    return entity;
                });

        if (Boolean.TRUE.equals(enrollment.getIsActive()) &&
                enrollment.getStatus() != EnrollmentStatus.DROPPED) {
            throw new BadRequestException("User already enrolled in this course");
        }

        enrollment.setStatus(EnrollmentStatus.ENROLLED);
        enrollment.setIsActive(true);
        enrollment.setEnrolledAt(OffsetDateTime.now());
        enrollment.setUpdated(OffsetDateTime.now());
        enrollment.setUpdatedBy(currentUserId);
        if (enrollment.getCreatedBy() == null) {
            enrollment.setCreatedBy(currentUserId);
        }
        enrollmentRepository.save(enrollment);
        log.info("User {} enrolled in course {}", currentUserId, courseId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ChapterResponse> getChapters(UUID courseId) {
        Course course = getActiveCourse(courseId);
        UUID currentUserId = UserContext.getCurrentUserId();
        boolean published = course.getStatus() == CourseStatus.PUBLISHED;
        boolean manager = canManageCourse(course, currentUserId);
        if (!published && !manager) {
            throw new NotFoundException("Course not found");
        }
        List<Progress> userProgress = currentUserId != null
                ? progressRepository.findByUserAndCourse(currentUserId, courseId)
                : Collections.emptyList();
        return buildChapterSnapshot(course, currentUserId, manager, userProgress).getChapters();
    }

    @Override
    public ChapterResponse createChapter(UUID courseId, ChapterRequest request) {
        Course course = getActiveCourse(courseId);
        UUID currentUserId = requireCurrentUser();
        ensureCanManage(course, currentUserId);

        Integer nextOrder = request.getOrderIndex();
        if (nextOrder == null) {
            Integer maxOrder = chapterRepository.findMaxOrderIndexByCourseId(courseId);
            nextOrder = (maxOrder == null ? 0 : maxOrder) + 1;
        }

        Chapter chapter = courseMapper.toChapterEntity(request, course, currentUserId, nextOrder);
        chapterRepository.save(chapter);
        log.info("Chapter {} created for course {}", chapter.getId(), courseId);
        return buildChapterResponseForManager(chapter, course);
    }

    @Override
    public ChapterResponse updateChapter(UUID courseId, UUID chapterId, ChapterRequest request) {
        Course course = getActiveCourse(courseId);
        UUID currentUserId = requireCurrentUser();
        ensureCanManage(course, currentUserId);

        Chapter chapter = chapterRepository.findByIdAndCourse_IdAndIsActiveTrue(chapterId, courseId)
                .orElseThrow(() -> new NotFoundException("Chapter not found"));

        courseMapper.updateChapter(chapter, request, currentUserId);
        chapterRepository.save(chapter);
        log.info("Chapter {} updated for course {}", chapterId, courseId);
        return buildChapterResponseForManager(chapter, course);
    }

    @Override
    public void deleteChapter(UUID courseId, UUID chapterId) {
        log.info("🗑️ START deleteChapter: courseId={}, chapterId={}", courseId, chapterId);

        Course course = getActiveCourse(courseId);
        UUID currentUserId = requireCurrentUser();
        ensureCanManage(course, currentUserId);

        Chapter chapter = chapterRepository.findByIdAndCourse_IdAndIsActiveTrue(chapterId, courseId)
                .orElseThrow(() -> new NotFoundException("Chapter not found"));

        log.info("🗑️ Found chapter to delete: id={}, orderIndex={}, title={}",
                chapter.getId(), chapter.getOrderIndex(), chapter.getTitle());

        // ✅ HARD DELETE - Xóa cứng luôn
        chapterRepository.delete(chapter);
        log.info("✅ Chapter {} hard-deleted (CASCADE will delete all lessons & assets)", chapterId);

        // ✅ AUTO REORDER: Update orderIndex of remaining chapters
        List<Chapter> remainingChapters = chapterRepository
                .findByCourse_IdAndIsActiveTrueOrderByOrderIndexAsc(courseId);

        log.info("🔄 Found {} remaining chapters to reorder", remainingChapters.size());

        if (!remainingChapters.isEmpty()) {
            int newOrder = 1;
            for (Chapter ch : remainingChapters) {
                if (!ch.getOrderIndex().equals(newOrder)) {
                    ch.setOrderIndex(newOrder);
                    ch.setUpdatedBy(currentUserId);
                    ch.setUpdated(OffsetDateTime.now());
                    log.info("📝 Reorder chapter {} from {} to {}", ch.getId(), ch.getOrderIndex(), newOrder);
                }
                newOrder++;
            }
            chapterRepository.saveAll(remainingChapters);
            log.info("✅ Reordered {} chapters successfully", remainingChapters.size());
        }

        log.info("🏁 DONE deleteChapter: {} remaining chapters in course {}",
                remainingChapters.size(), courseId);
    }

    @Override
    public LessonResponse getLesson(UUID courseId, UUID chapterId, UUID lessonId) {
        Course course = getActiveCourse(courseId);
        resolveChapter(courseId, chapterId);

        Lesson lesson = lessonRepository.findByIdAndChapter_IdAndIsActiveTrue(lessonId, chapterId)
                .orElseThrow(() -> new NotFoundException("Lesson not found"));

        log.info("Lesson {} retrieved from chapter {}", lessonId, chapterId);
        return buildLessonResponse(lesson, course, null);
    }

    @Override
    public LessonResponse createLesson(UUID courseId, UUID chapterId, LessonRequest request) {
        Course course = getActiveCourse(courseId);
        UUID currentUserId = requireCurrentUser();
        ensureCanManage(course, currentUserId);
        Chapter chapter = resolveChapter(courseId, chapterId);

        Integer orderIndex = request.getOrderIndex();
        if (orderIndex == null) {
            Integer maxOrder = lessonRepository.findMaxOrderIndexByChapterId(chapterId);
            orderIndex = (maxOrder == null ? 0 : maxOrder) + 1;
        }

        Lesson lesson = courseMapper.toLessonEntity(request, chapter, currentUserId, orderIndex);
        lessonRepository.save(lesson);
        log.info("Lesson {} created in chapter {}", lesson.getId(), chapterId);
        return buildLessonResponse(lesson, course, null);
    }

    @Override
    public LessonResponse updateLesson(UUID courseId, UUID chapterId, UUID lessonId, LessonRequest request) {
        Course course = getActiveCourse(courseId);
        UUID currentUserId = requireCurrentUser();
        ensureCanManage(course, currentUserId);
        Chapter chapter = resolveChapter(courseId, chapterId);

        Lesson lesson = lessonRepository.findByIdAndChapter_IdAndIsActiveTrue(lessonId, chapterId)
                .orElseThrow(() -> new NotFoundException("Lesson not found"));

        courseMapper.updateLesson(lesson, request, currentUserId);
        lessonRepository.save(lesson);
        log.info("Lesson {} updated in chapter {}", lessonId, chapterId);
        return buildLessonResponse(lesson, course, null);
    }

    @Override
    public void deleteLesson(UUID courseId, UUID chapterId, UUID lessonId) {
        log.info("🗑️ START deleteLesson: courseId={}, chapterId={}, lessonId={}", courseId, chapterId, lessonId);

        Course course = getActiveCourse(courseId);
        UUID currentUserId = requireCurrentUser();
        ensureCanManage(course, currentUserId);
        resolveChapter(courseId, chapterId);

        Lesson lesson = lessonRepository.findByIdAndChapter_IdAndIsActiveTrue(lessonId, chapterId)
                .orElseThrow(() -> new NotFoundException("Lesson not found"));

        log.info("🗑️ Found lesson to delete: id={}, orderIndex={}, title={}",
                lesson.getId(), lesson.getOrderIndex(), lesson.getTitle());

        // ✅ HARD DELETE - Xóa cứng luôn
        lessonRepository.delete(lesson);
        log.info("✅ Lesson {} hard-deleted (CASCADE will delete all assets & progress)", lessonId);

        // ✅ AUTO REORDER: Update orderIndex of remaining lessons
        List<Lesson> remainingLessons = lessonRepository
                .findByChapter_IdAndIsActiveTrueOrderByOrderIndexAsc(chapterId);

        log.info("🔄 Found {} remaining lessons to reorder", remainingLessons.size());

        if (!remainingLessons.isEmpty()) {
            int newOrder = 1;
            for (Lesson l : remainingLessons) {
                if (!l.getOrderIndex().equals(newOrder)) {
                    l.setOrderIndex(newOrder);
                    l.setUpdatedBy(currentUserId);
                    l.setUpdated(OffsetDateTime.now());
                    log.info("📝 Reorder lesson {} from {} to {}", l.getId(), l.getOrderIndex(), newOrder);
                }
                newOrder++;
            }
            lessonRepository.saveAll(remainingLessons);
            log.info("✅ Reordered {} lessons successfully", remainingLessons.size());
        }

        log.info("🏁 DONE deleteLesson: {} remaining lessons in chapter {}",
                remainingLessons.size(), chapterId);
    }

    @Override
    public LessonAssetResponse addLessonAsset(UUID courseId, UUID chapterId, UUID lessonId,
            LessonAssetRequest request) {
        Course course = getActiveCourse(courseId);
        UUID currentUserId = requireCurrentUser();
        ensureCanManage(course, currentUserId);
        Lesson lesson = resolveLesson(courseId, chapterId, lessonId);

        validateLessonAssetRequest(request);

        Integer orderIndex = request.getOrderIndex();
        if (orderIndex == null) {
            Integer maxOrder = lessonAssetRepository.findMaxOrderIndexByLessonId(lessonId);
            orderIndex = (maxOrder == null ? 0 : maxOrder) + 1;
        }

        LessonAsset asset = courseMapper.toLessonAssetEntity(request, lesson, currentUserId, orderIndex);
        lessonAssetRepository.save(asset);
        log.info("Asset {} created for lesson {}", asset.getId(), lessonId);
        return buildAssetResponse(asset, course);
    }

    @Override
    public LessonAssetResponse updateLessonAsset(UUID courseId, UUID chapterId, UUID lessonId, UUID assetId,
            LessonAssetRequest request) {
        Course course = getActiveCourse(courseId);
        UUID currentUserId = requireCurrentUser();
        ensureCanManage(course, currentUserId);
        resolveLesson(courseId, chapterId, lessonId);

        LessonAsset asset = lessonAssetRepository.findByIdAndLesson_IdAndIsActiveTrue(assetId, lessonId)
                .orElseThrow(() -> new NotFoundException("Lesson asset not found"));

        if (request.getFileId() == null && request.getExternalUrl() == null && asset.getFileId() == null &&
                asset.getExternalUrl() == null && asset.getAssetType() != LessonAssetType.CODE_TEMPLATE) {
            throw new BadRequestException("Asset must include a file or an external URL");
        }

        courseMapper.updateLessonAsset(asset, request, currentUserId);
        lessonAssetRepository.save(asset);
        log.info("Asset {} updated for lesson {}", assetId, lessonId);
        return buildAssetResponse(asset, course);
    }

    @Override
    public void deleteLessonAsset(UUID courseId, UUID chapterId, UUID lessonId, UUID assetId) {
        Course course = getActiveCourse(courseId);
        UUID currentUserId = requireCurrentUser();
        ensureCanManage(course, currentUserId);
        resolveLesson(courseId, chapterId, lessonId);

        LessonAsset asset = lessonAssetRepository.findByIdAndLesson_IdAndIsActiveTrue(assetId, lessonId)
                .orElseThrow(() -> new NotFoundException("Lesson asset not found"));

        asset.setIsActive(false);
        asset.setUpdatedBy(currentUserId);
        asset.setUpdated(OffsetDateTime.now());
        lessonAssetRepository.save(asset);
        log.info("Asset {} soft-deleted for lesson {}", assetId, lessonId);
    }

    private Lesson resolveLesson(UUID courseId, UUID chapterId, UUID lessonId) {
        resolveChapter(courseId, chapterId);
        return lessonRepository.findByIdAndChapter_IdAndIsActiveTrue(lessonId, chapterId)
                .orElseThrow(() -> new NotFoundException("Lesson not found"));
    }

    private Chapter resolveChapter(UUID courseId, UUID chapterId) {
        return chapterRepository.findByIdAndCourse_IdAndIsActiveTrue(chapterId, courseId)
                .orElseThrow(() -> new NotFoundException("Chapter not found"));
    }

    private CourseSummaryResponse buildCourseSummary(Course course) {
        CourseFileResource thumbnail = buildFileResourceFromUrl(course.getThumbnail());
        CourseFileResource intro = buildFileResourceFromUrl(course.getIntroVideoFile());
        long totalEnrollments = enrollmentRepository.countByCourseAndIsActiveTrue(course);
        Double averageRating = ratingRepository.getAverageScore(course.getId(), RatingTarget.COURSE.name());
        long ratingCount = ratingRepository.countByTargetIdAndTargetTypeAndIsActiveTrue(course.getId(),
                RatingTarget.COURSE);
        return CourseSummaryResponse.builder()
                .id(course.getId())
                .title(course.getTitle())
                .description(course.getDescription())
                .price(course.getPrice())
                .discountPrice(course.getDiscountPrice())
                .promoEndDate(course.getPromoEndDate())
                .status(course.getStatus())
                .level(course.getLevel())
                .language(course.getLanguage())
                .skills(course.getCourseSkills() != null ? course.getCourseSkills().stream()
                        .map(cs -> {
                            SkillDTO dto = null;
                            if (cs.getSkill() != null) {
                                dto = new SkillDTO(
                                        cs.getSkill().getId(),
                                        cs.getSkill().getName(),
                                        cs.getSkill().getThumbnail(),
                                        cs.getSkill().getCategory());
                            }
                            return dto;
                        })
                        .filter(java.util.Objects::nonNull)
                        .collect(java.util.stream.Collectors.toList())
                        : java.util.Collections.emptyList())
                .tags(course.getCourseTags() != null ? course.getCourseTags().stream()
                        .map(ct -> {
                            TagDTO dto = null;
                            if (ct.getTag() != null) {
                                dto = new TagDTO(
                                        ct.getTag().getId(),
                                        ct.getTag().getName());
                            }
                            return dto;
                        })
                        .filter(java.util.Objects::nonNull)
                        .collect(java.util.stream.Collectors.toList())
                        : java.util.Collections.emptyList())
                .objectives(course.getObjectives() != null ? List.copyOf(course.getObjectives()) : List.of())
                .requirements(course.getRequirements() != null ? List.copyOf(course.getRequirements()) : List.of())
                .instructorId(course.getInstructorId())
                .thumbnail(thumbnail)
                .introVideo(intro)
                .created(course.getCreated())
                .updated(course.getUpdated())
                .active(course.getIsActive())
                .totalEnrollments(totalEnrollments)
                .averageRating(averageRating != null ? Math.round(averageRating * 100d) / 100d : null)
                .ratingCount(ratingCount)
                .build();
    }

    private CourseProgressSnapshot buildChapterSnapshot(Course course, UUID userId, boolean isManager,
            List<Progress> userProgress) {
        Map<UUID, Progress> progressByLesson = userProgress.stream()
                .filter(progress -> progress.getLesson() != null)
                .collect(Collectors.toMap(progress -> progress.getLesson().getId(), Function.identity(),
                        (left, right) -> left));

        List<Chapter> chapterEntities = chapterRepository
                .findByCourse_IdAndIsActiveTrueOrderByOrderIndexAsc(course.getId());
        List<ChapterResponse> chapterResponses = new ArrayList<>();
        List<UUID> unlockedChapters = new ArrayList<>();
        List<UUID> lockedChapters = new ArrayList<>();

        long totalLessons = 0;
        long totalEstimatedMinutes = 0;
        double totalMandatoryWeight = 0;
        double totalCompletedWeight = 0;
        long completedLessons = 0;

        boolean previousUnlocked = true;
        double previousCompletion = 1d;
        UUID currentChapterId = null;

        for (int index = 0; index < chapterEntities.size(); index++) {
            Chapter chapter = chapterEntities.get(index);
            List<Lesson> lessonEntities = lessonRepository
                    .findByChapter_IdAndIsActiveTrueOrderByOrderIndexAsc(chapter.getId());

            List<LessonResponse> lessonResponses = new ArrayList<>();
            double chapterMandatoryWeight = 0;
            double chapterCompletedWeight = 0;

            for (Lesson lesson : lessonEntities) {
                Progress progress = progressByLesson.get(lesson.getId());
                LessonResponse lessonResponse = buildLessonResponse(lesson, course, progress);
                lessonResponses.add(lessonResponse);

                boolean mandatory = lesson.getMandatory() == null || lesson.getMandatory();
                float weight = lesson.getCompletionWeight() != null ? lesson.getCompletionWeight() : 1f;
                float completionValue = lessonResponse.getCompletion() != null
                        ? Math.max(0f, Math.min(1f, lessonResponse.getCompletion()))
                        : 0f;

                totalLessons++;
                if (lesson.getEstimatedDuration() != null) {
                    totalEstimatedMinutes += lesson.getEstimatedDuration();
                }
                if (Boolean.TRUE.equals(lessonResponse.getCompleted())) {
                    completedLessons++;
                }

                if (mandatory) {
                    chapterMandatoryWeight += weight;
                    totalMandatoryWeight += weight;
                    chapterCompletedWeight += weight * completionValue;
                    totalCompletedWeight += weight * completionValue;
                }
            }

            double chapterCompletionRatio = chapterMandatoryWeight == 0 ? 1d
                    : chapterCompletedWeight / chapterMandatoryWeight;
            chapterCompletionRatio = Math.min(1d, Math.max(0d, chapterCompletionRatio));

            boolean baseLocked = chapter.getLocked() != null ? chapter.getLocked() : Boolean.FALSE;
            boolean unlocked = isManager || !baseLocked;

            if (!unlocked) {
                double threshold = chapter.getMinCompletionThreshold() != null ? chapter.getMinCompletionThreshold()
                        : 0.7d;
                if (index == 0) {
                    unlocked = true;
                } else if (Boolean.TRUE.equals(chapter.getAutoUnlock())) {
                    unlocked = previousUnlocked && previousCompletion >= threshold;
                }
            }

            if (unlocked) {
                unlockedChapters.add(chapter.getId());
                if (currentChapterId == null && chapterCompletionRatio < 1d) {
                    currentChapterId = chapter.getId();
                }
            } else {
                lockedChapters.add(chapter.getId());
            }

            if (!unlocked) {
                lessonResponses.forEach(lessonResponse -> {
                    lessonResponse.setAssets(List.of());
                    lessonResponse.setVideoUrl(null);
                    lessonResponse.setDocumentUrls(List.of());
                });
            }

            ChapterResponse chapterResponse = ChapterResponse.builder()
                    .id(chapter.getId())
                    .title(chapter.getTitle())
                    .orderIndex(chapter.getOrderIndex())
                    .minCompletionThreshold(chapter.getMinCompletionThreshold())
                    .autoUnlock(chapter.getAutoUnlock())
                    .locked(!unlocked)
                    .unlocked(unlocked)
                    .completionRatio(chapterCompletionRatio)
                    .currentChapter(false)
                    .created(chapter.getCreated())
                    .updated(chapter.getUpdated())
                    .lessons(lessonResponses)
                    .build();

            chapterResponses.add(chapterResponse);

            previousUnlocked = unlocked;
            previousCompletion = chapterCompletionRatio;
        }

        if (currentChapterId == null && !unlockedChapters.isEmpty()) {
            currentChapterId = unlockedChapters.get(unlockedChapters.size() - 1);
        } else if (currentChapterId == null && !chapterResponses.isEmpty()) {
            currentChapterId = chapterResponses.get(0).getId();
        }

        for (ChapterResponse response : chapterResponses) {
            response.setCurrentChapter(response.getId().equals(currentChapterId));
        }

        double overallProgress = totalMandatoryWeight == 0 ? 1d : totalCompletedWeight / totalMandatoryWeight;
        overallProgress = Math.min(1d, Math.max(0d, overallProgress));

        return new CourseProgressSnapshot(
                chapterResponses,
                totalLessons,
                totalEstimatedMinutes,
                overallProgress,
                currentChapterId,
                unlockedChapters,
                lockedChapters,
                completedLessons);
    }

    private ChapterResponse buildChapterResponseForManager(Chapter chapter, Course course) {
        List<LessonResponse> lessons = lessonRepository
                .findByChapter_IdAndIsActiveTrueOrderByOrderIndexAsc(chapter.getId()).stream()
                .map(lesson -> buildLessonResponse(lesson, course, null))
                .collect(Collectors.toList());

        boolean locked = chapter.getLocked() != null ? chapter.getLocked() : Boolean.FALSE;
        return ChapterResponse.builder()
                .id(chapter.getId())
                .title(chapter.getTitle())
                .orderIndex(chapter.getOrderIndex())
                .minCompletionThreshold(chapter.getMinCompletionThreshold())
                .autoUnlock(chapter.getAutoUnlock())
                .locked(locked)
                .unlocked(!locked)
                .completionRatio(null)
                .currentChapter(false)
                .created(chapter.getCreated())
                .updated(chapter.getUpdated())
                .lessons(lessons)
                .build();
    }

    private LessonResponse buildLessonResponse(Lesson lesson, Course course, Progress progress) {
        List<LessonAssetResponse> assets = lessonAssetRepository
                .findByLesson_IdAndIsActiveTrueOrderByOrderIndexAsc(lesson.getId()).stream()
                .map(asset -> buildAssetResponse(asset, course))
                .collect(Collectors.toList());

        Float completion = progress != null && progress.getCompletion() != null
                ? Math.max(0f, Math.min(1f, progress.getCompletion()))
                : 0f;
        boolean completed = progress != null && (progress.isCompleted()
                || progress.getCompletedAt() != null
                || (progress.getCompletion() != null && progress.getCompletion() >= 1f));

        return LessonResponse.builder()
                .id(lesson.getId())
                .title(lesson.getTitle())
                .description(lesson.getDescription())
                .orderIndex(lesson.getOrderIndex())
                .contentType(lesson.getContentType())
                .content(lesson.getContent())
                .isFree(lesson.getIsFree())
                .mandatory(lesson.getMandatory())
                .completionWeight(lesson.getCompletionWeight())
                .estimatedDuration(lesson.getEstimatedDuration())
                .workspaceEnabled(lesson.getWorkspaceEnabled())
                .workspaceLanguages(lesson.getWorkspaceLanguages() != null ? List.copyOf(lesson.getWorkspaceLanguages())
                        : List.of())
                .workspaceTemplate(lesson.getWorkspaceTemplate())
                .videoUrl(lesson.getVideoUrl())
                .documentUrls(lesson.getDocumentUrls() != null ? List.copyOf(lesson.getDocumentUrls()) : List.of())
                .created(lesson.getCreated())
                .updated(lesson.getUpdated())
                .assets(assets)
                .completion(completion)
                .completed(completed)
                .completedAt(progress != null ? progress.getCompletedAt() : null)
                .progressUpdatedAt(progress != null ? progress.getUpdated() : null)
                .build();
    }

    private LessonAssetResponse buildAssetResponse(LessonAsset asset, Course course) {
        CourseFileResource fileResource = buildFileResource(asset.getFileId());
        return LessonAssetResponse.builder()
                .id(asset.getId())
                .assetType(asset.getAssetType())
                .orderIndex(asset.getOrderIndex())
                .title(asset.getTitle())
                .description(asset.getDescription())
                .file(fileResource)
                .externalUrl(asset.getExternalUrl())
                .metadata(asset.getMetadata())
                .created(asset.getCreated())
                .updated(asset.getUpdated())
                .build();
    }

    private CourseFileResource buildFileResource(UUID fileId) {
        if (fileId == null) {
            return null;
        }
        return CourseFileResource.builder().fileId(fileId).build();
    }

    private CourseFileResource buildFileResourceFromUrl(String url) {
        if (url == null || url.trim().isEmpty()) {
            return null;
        }
        return CourseFileResource.builder().url(url).build();
    }

    private Course getActiveCourse(UUID courseId) {
        return courseRepository.findByIdAndIsActiveTrue(courseId)
                .orElseThrow(() -> new NotFoundException("Course not found"));
    }

    private boolean canManageCourse(Course course, UUID userId) {
        if (userId == null) {
            return false;
        }
        return userId.equals(course.getInstructorId()) || UserContext.hasAnyRole(ROLE_ADMIN);
    }

    private void ensureCanManage(Course course, UUID currentUserId) {
        if (!canManageCourse(course, currentUserId)) {
            throw new ForbiddenException("You are not allowed to modify this course");
        }
    }

    private UUID requireCurrentUser() {
        UUID currentUserId = UserContext.getCurrentUserId();
        if (currentUserId == null) {
            throw new UnauthorizedException("Authentication required");
        }
        return currentUserId;
    }

    private void ensureInstructorOrAdmin() {
        if (!UserContext.hasAnyRole(ROLE_ADMIN, ROLE_INSTRUCTOR)) {
            throw new ForbiddenException("Only instructors or admins can perform this action");
        }
    }

    private UUID resolveInstructorId(UUID requestedInstructorId, UUID currentUserId) {
        if (requestedInstructorId == null || requestedInstructorId.equals(currentUserId)) {
            return currentUserId;
        }
        if (!UserContext.hasAnyRole(ROLE_ADMIN)) {
            throw new ForbiddenException("Only admins can assign courses to other instructors");
        }
        return requestedInstructorId;
    }

    private void validateDiscount(BigDecimal price, BigDecimal discountPrice) {
        if (discountPrice == null) {
            return;
        }
        if (price == null) {
            throw new BadRequestException("Price must be provided when discount price is set");
        }
        if (discountPrice.compareTo(BigDecimal.ZERO) < 0) {
            throw new BadRequestException("Discount price cannot be negative");
        }
        if (discountPrice.compareTo(price) > 0) {
            throw new BadRequestException("Discount price cannot exceed course price");
        }
    }

    private void validateLessonAssetRequest(LessonAssetRequest request) {
        if (request.getAssetType() == null) {
            throw new BadRequestException("Asset type is required");
        }
        if (request.getAssetType() != LessonAssetType.CODE_TEMPLATE &&
                request.getFileId() == null &&
                (request.getExternalUrl() == null || request.getExternalUrl().isBlank())) {
            throw new BadRequestException("Asset must include a file or an external URL");
        }
    }

    private static final class CourseProgressSnapshot {
        private final List<ChapterResponse> chapters;
        private final long totalLessons;
        private final long totalEstimatedDurationMinutes;
        private final double overallProgress;
        private final UUID currentChapterId;
        private final List<UUID> unlockedChapterIds;
        private final List<UUID> lockedChapterIds;
        private final long completedLessons;

        CourseProgressSnapshot(List<ChapterResponse> chapters,
                long totalLessons,
                long totalEstimatedDurationMinutes,
                double overallProgress,
                UUID currentChapterId,
                List<UUID> unlockedChapterIds,
                List<UUID> lockedChapterIds,
                long completedLessons) {
            this.chapters = chapters;
            this.totalLessons = totalLessons;
            this.totalEstimatedDurationMinutes = totalEstimatedDurationMinutes;
            this.overallProgress = overallProgress;
            this.currentChapterId = currentChapterId;
            this.unlockedChapterIds = unlockedChapterIds;
            this.lockedChapterIds = lockedChapterIds;
            this.completedLessons = completedLessons;
        }

        public List<ChapterResponse> getChapters() {
            return chapters;
        }

        public long getTotalLessons() {
            return totalLessons;
        }

        public long getTotalEstimatedDurationMinutes() {
            return totalEstimatedDurationMinutes;
        }

        public double getOverallProgress() {
            return overallProgress;
        }

        public UUID getCurrentChapterId() {
            return currentChapterId;
        }

        public List<UUID> getUnlockedChapterIds() {
            return unlockedChapterIds;
        }

        public List<UUID> getLockedChapterIds() {
            return lockedChapterIds;
        }

        public long getCompletedLessons() {
            return completedLessons;
        }
    }

    private void mapSkillsToCourse(Course course, List<String> skillNames) {
        log.info("========== mapSkillsToCourse START ==========");
        log.info("mapSkillsToCourse - Input skillNames: {}", skillNames);
        log.info("mapSkillsToCourse - Course ID: {}", course.getId());
        log.info("mapSkillsToCourse - Initial course skills count: {}", course.getCourseSkills().size());

        // Build a set of skill names to add (normalized)
        java.util.Set<String> requestedSkillNames = new java.util.HashSet<>();
        if (skillNames != null && !skillNames.isEmpty()) {
            for (String name : skillNames) {
                if (name != null && !name.trim().isEmpty()) {
                    requestedSkillNames.add(name.trim());
                }
            }
        }
        log.info("mapSkillsToCourse: Requested skill names (normalized): {}", requestedSkillNames);

        // Remove skills that are not in the requested list
        java.util.Iterator<CourseSkill> iterator = course.getCourseSkills().iterator();
        while (iterator.hasNext()) {
            CourseSkill cs = iterator.next();
            String existingSkillName = cs.getSkill() != null ? cs.getSkill().getName() : null;
            if (existingSkillName == null || !requestedSkillNames.contains(existingSkillName)) {
                log.info("mapSkillsToCourse: Removing skill: {}", existingSkillName);
                iterator.remove();
            } else {
                // Skill already exists, remove from requested set to avoid duplicate
                log.info("mapSkillsToCourse: Skill '{}' already exists, skipping", existingSkillName);
                requestedSkillNames.remove(existingSkillName);
            }
        }
        log.info("mapSkillsToCourse: After cleanup, course skills count: {}", course.getCourseSkills().size());
        log.info("mapSkillsToCourse: Skills to add: {}", requestedSkillNames);

        // Add new skills that don't exist yet
        for (String skillName : requestedSkillNames) {
            log.info("mapSkillsToCourse: Processing new skill: '{}'", skillName);

            Skill skill = skillRepository.findByName(skillName)
                    .orElseGet(() -> {
                        log.info("mapSkillsToCourse: Skill '{}' not found, creating new", skillName);
                        Skill newSkill = new Skill();
                        newSkill.setName(skillName);
                        Skill saved = skillRepository.save(newSkill);
                        log.info("mapSkillsToCourse: Created skill ID: {}, name: '{}'", saved.getId(), saved.getName());
                        return saved;
                    });

            log.info("mapSkillsToCourse: Adding skill {} (ID: {}) to course", skill.getName(), skill.getId());

            CourseSkill courseSkill = new CourseSkill();
            courseSkill.setCourse(course);
            courseSkill.setSkill(skill);
            courseSkill.setAssignedAt(OffsetDateTime.now());
            course.getCourseSkills().add(courseSkill);
            log.info("mapSkillsToCourse: Added CourseSkill for skill: {}", skill.getName());
        }

        log.info("mapSkillsToCourse: Final course skills count: {}", course.getCourseSkills().size());
        log.info("========== mapSkillsToCourse END ==========");
    }

    private void mapTagsToCourse(Course course, List<String> tagNames) {
        log.info("========== mapTagsToCourse START ==========");
        log.info("mapTagsToCourse - Input tagNames: {}", tagNames);
        log.info("mapTagsToCourse - Course ID: {}", course.getId());
        log.info("mapTagsToCourse - Initial course tags count: {}", course.getCourseTags().size());

        // Build a set of tag names to add (normalized)
        java.util.Set<String> requestedTagNames = new java.util.HashSet<>();
        if (tagNames != null && !tagNames.isEmpty()) {
            for (String name : tagNames) {
                if (name != null && !name.trim().isEmpty()) {
                    requestedTagNames.add(name.trim());
                }
            }
        }
        log.info("mapTagsToCourse: Requested tag names (normalized): {}", requestedTagNames);

        // Remove tags that are not in the requested list
        java.util.Iterator<CourseTag> iterator = course.getCourseTags().iterator();
        while (iterator.hasNext()) {
            CourseTag ct = iterator.next();
            String existingTagName = ct.getTag() != null ? ct.getTag().getName() : null;
            if (existingTagName == null || !requestedTagNames.contains(existingTagName)) {
                log.info("mapTagsToCourse: Removing tag: {}", existingTagName);
                iterator.remove();
            } else {
                // Tag already exists, remove from requested set to avoid duplicate
                log.info("mapTagsToCourse: Tag '{}' already exists, skipping", existingTagName);
                requestedTagNames.remove(existingTagName);
            }
        }
        log.info("mapTagsToCourse: After cleanup, course tags count: {}", course.getCourseTags().size());
        log.info("mapTagsToCourse: Tags to add: {}", requestedTagNames);

        // Add new tags that don't exist yet
        for (String tagName : requestedTagNames) {
            log.info("mapTagsToCourse: Processing new tag: '{}'", tagName);

            Tag tag = tagRepository.findByName(tagName)
                    .orElseGet(() -> {
                        log.info("mapTagsToCourse: Tag '{}' not found, creating new", tagName);
                        Tag newTag = new Tag();
                        newTag.setName(tagName);
                        OffsetDateTime now = OffsetDateTime.now();
                        newTag.setCreated(now);
                        newTag.setUpdated(now);
                        Tag saved = tagRepository.save(newTag);
                        log.info("mapTagsToCourse: Created tag ID: {}, name: '{}'", saved.getId(), saved.getName());
                        return saved;
                    });

            log.info("mapTagsToCourse: Adding tag {} (ID: {}) to course", tag.getName(), tag.getId());

            CourseTag courseTag = new CourseTag();
            courseTag.setCourse(course);
            courseTag.setTag(tag);
            courseTag.setAssignedAt(OffsetDateTime.now());
            course.getCourseTags().add(courseTag);
            log.info("mapTagsToCourse: Added CourseTag for tag: {}", tag.getName());
        }

        log.info("mapTagsToCourse: Final course tags count: {}", course.getCourseTags().size());
        log.info("========== mapTagsToCourse END ==========");
    }

    private String normalizeSearch(String search) {
        if (search == null) {
            return null;
        }
        String trimmed = search.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    // ===== Event Publishing Helper Methods =====

    private void publishCourseCreatedEvent(Course course) {
        try {
            CourseEvent event = CourseEvent.builder()
                    .eventType(CourseEvent.EventType.CREATED)
                    .courseId(course.getId())
                    .title(course.getTitle())
                    .description(course.getDescription())
                    .objectives(course.getObjectives() != null ? course.getObjectives().toString() : null)
                    .requirements(course.getRequirements() != null ? course.getRequirements().toString() : null)
                    .level(course.getLevel() != null ? course.getLevel().name() : null)
                    .language(course.getLanguage() != null ? course.getLanguage().name() : null)
                    .instructorId(course.getInstructorId())
                    .status(course.getStatus() != null ? course.getStatus().name() : null)
                    .build();
            eventPublisher.publishCourseEvent(event);
        } catch (Exception e) {
            log.error("Failed to publish course created event for course {}", course.getId(), e);
        }
    }

    private void publishCourseUpdatedEvent(Course course) {
        try {
            CourseEvent event = CourseEvent.builder()
                    .eventType(CourseEvent.EventType.UPDATED)
                    .courseId(course.getId())
                    .title(course.getTitle())
                    .description(course.getDescription())
                    .objectives(course.getObjectives() != null ? course.getObjectives().toString() : null)
                    .requirements(course.getRequirements() != null ? course.getRequirements().toString() : null)
                    .level(course.getLevel() != null ? course.getLevel().name() : null)
                    .language(course.getLanguage() != null ? course.getLanguage().name() : null)
                    .instructorId(course.getInstructorId())
                    .status(course.getStatus() != null ? course.getStatus().name() : null)
                    .build();
            eventPublisher.publishCourseEvent(event);
        } catch (Exception e) {
            log.error("Failed to publish course updated event for course {}", course.getId(), e);
        }
    }

    private void publishCourseDeletedEvent(Course course) {
        try {
            CourseEvent event = CourseEvent.builder()
                    .eventType(CourseEvent.EventType.DELETED)
                    .courseId(course.getId())
                    .title(course.getTitle())
                    .build();
            eventPublisher.publishCourseEvent(event);
        } catch (Exception e) {
            log.error("Failed to publish course deleted event for course {}", course.getId(), e);
        }
    }

    /**
     * Prefer explicit `skills` field in request. If absent, fallback to
     * `categories`.
     */
    private List<String> chooseSkills(CourseRequest request) {
        if (request == null)
            return Collections.emptyList();
        if (request.getSkills() != null)
            return request.getSkills();
        return request.getCategories() != null ? request.getCategories() : Collections.emptyList();
    }
}
