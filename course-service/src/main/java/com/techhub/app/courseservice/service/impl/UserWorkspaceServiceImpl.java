package com.techhub.app.courseservice.service.impl;

import com.techhub.app.commonservice.context.UserContext;
import com.techhub.app.commonservice.exception.ForbiddenException;
import com.techhub.app.commonservice.exception.NotFoundException;
import com.techhub.app.commonservice.exception.UnauthorizedException;
import com.techhub.app.courseservice.dto.request.UserCodeRequest;
import com.techhub.app.courseservice.dto.response.UserCodeResponse;
import com.techhub.app.courseservice.entity.Chapter;
import com.techhub.app.courseservice.entity.Course;
import com.techhub.app.courseservice.entity.Lesson;
import com.techhub.app.courseservice.entity.Progress;
import com.techhub.app.courseservice.entity.UserCode;
import com.techhub.app.courseservice.repository.EnrollmentRepository;
import com.techhub.app.courseservice.repository.LessonRepository;
import com.techhub.app.courseservice.repository.ProgressRepository;
import com.techhub.app.courseservice.repository.UserCodeRepository;
import com.techhub.app.courseservice.service.UserWorkspaceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class UserWorkspaceServiceImpl implements UserWorkspaceService {

    private final LessonRepository lessonRepository;
    private final UserCodeRepository userCodeRepository;
    private final ProgressRepository progressRepository;
    private final EnrollmentRepository enrollmentRepository;

    @Override
    @Transactional(readOnly = true)
    public UserCodeResponse getUserCode(UUID courseId, UUID lessonId) {
        UUID userId = requireCurrentUser();
        Lesson lesson = resolveLesson(courseId, lessonId);
        ensureAccess(lesson.getChapter().getCourse(), userId);

        return userCodeRepository.findByUserIdAndLesson_Id(userId, lessonId)
                .map(this::mapToResponse)
                .orElse(UserCodeResponse.builder()
                        .lessonId(lessonId)
                        .userId(userId)
                        .language(lesson.getWorkspaceLanguages() != null && !lesson.getWorkspaceLanguages().isEmpty()
                                ? lesson.getWorkspaceLanguages().get(0)
                                : "plaintext")
                        .code("")
                        .savedAt(null)
                        .build());
    }

    @Override
    public UserCodeResponse saveUserCode(UUID courseId, UUID lessonId, UserCodeRequest request) {
        UUID userId = requireCurrentUser();
        Lesson lesson = resolveLesson(courseId, lessonId);
        ensureAccess(lesson.getChapter().getCourse(), userId);

        UserCode userCode = userCodeRepository.findByUserIdAndLesson_Id(userId, lessonId)
                .orElseGet(() -> {
                    UserCode entity = new UserCode();
                    entity.setUserId(userId);
                    entity.setLesson(lesson);
                    entity.setCreatedBy(userId);
                    entity.setIsActive(true);
                    return entity;
                });

        userCode.setLanguage(request.getLanguage());
        userCode.setCode(request.getCode());
        userCode.setUpdatedBy(userId);
        userCodeRepository.save(userCode);
        log.debug("Workspace code saved for lesson {} by {}", lessonId, userId);

        Progress progress = progressRepository.findByUserIdAndLessonId(userId, lessonId)
                .orElseGet(() -> {
                    Progress entity = new Progress();
                    entity.setUserId(userId);
                    entity.setLesson(lesson);
                    entity.setCreatedBy(userId);
                    return entity;
                });
        float existingCompletion = progress.getCompletion() != null ? progress.getCompletion() : 0f;
        float newCompletion = Math.max(existingCompletion, 0.5f);
        progress.setCompletion(newCompletion);
        progress.setUpdatedBy(userId);
        progressRepository.save(progress);

        return mapToResponse(userCode);
    }

    private UserCodeResponse mapToResponse(UserCode userCode) {
        return UserCodeResponse.builder()
                .lessonId(userCode.getLesson().getId())
                .userId(userCode.getUserId())
                .language(userCode.getLanguage())
                .code(userCode.getCode())
                .savedAt(userCode.getSavedAt())
                .build();
    }

    private Lesson resolveLesson(UUID courseId, UUID lessonId) {
        Lesson lesson = lessonRepository.findById(lessonId)
                .orElseThrow(() -> new NotFoundException("Lesson not found"));
        Chapter chapter = lesson.getChapter();
        if (chapter == null || chapter.getCourse() == null) {
            throw new NotFoundException("Lesson is not attached to a course");
        }
        if (!chapter.getCourse().getId().equals(courseId)) {
            throw new ForbiddenException("Lesson does not belong to the specified course");
        }
        return lesson;
    }

    private void ensureAccess(Course course, UUID userId) {
        if (course.getInstructorId() != null && course.getInstructorId().equals(userId)) {
            return;
        }
        if (UserContext.hasAnyRole("ADMIN")) {
            return;
        }
        boolean enrolled = enrollmentRepository.findByUserIdAndCourse_IdAndIsActiveTrue(userId, course.getId()).isPresent();
        if (!enrolled) {
            throw new ForbiddenException("User is not enrolled in this course");
        }
    }

    private UUID requireCurrentUser() {
        UUID userId = UserContext.getCurrentUserId();
        if (userId == null) {
            throw new UnauthorizedException("Authentication required");
        }
        return userId;
    }
}
