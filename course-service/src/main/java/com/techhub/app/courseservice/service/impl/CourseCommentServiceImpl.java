package com.techhub.app.courseservice.service.impl;

import com.techhub.app.commonservice.context.UserContext;
import com.techhub.app.commonservice.exception.ForbiddenException;
import com.techhub.app.commonservice.exception.NotFoundException;
import com.techhub.app.commonservice.exception.UnauthorizedException;
import com.techhub.app.courseservice.dto.request.CommentRequest;
import com.techhub.app.courseservice.dto.response.CommentResponse;
import com.techhub.app.courseservice.entity.Comment;
import com.techhub.app.courseservice.entity.Course;
import com.techhub.app.courseservice.entity.Lesson;
import com.techhub.app.courseservice.enums.CommentTarget;
import com.techhub.app.courseservice.repository.CommentRepository;
import com.techhub.app.courseservice.repository.CourseRepository;
import com.techhub.app.courseservice.repository.LessonRepository;
import com.techhub.app.courseservice.service.CourseCommentService;
import com.techhub.app.courseservice.websocket.service.CommentWebSocketService;
import com.techhub.app.commonservice.websocket.dto.CommentTargetType;
import com.techhub.app.commonservice.websocket.dto.CommentWebSocketMessage;
import com.techhub.app.commonservice.websocket.dto.CommentEventType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class CourseCommentServiceImpl implements CourseCommentService {

    private final CommentRepository commentRepository;
    private final CourseRepository courseRepository;
    private final LessonRepository lessonRepository;
    private final CommentWebSocketService webSocketService;

    @Override
    @Transactional(readOnly = true)
    public List<CommentResponse> getCourseComments(UUID courseId) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new NotFoundException("Course not found"));
        List<Comment> comments = commentRepository.findAllByTarget(course.getId(), CommentTarget.COURSE);
        return buildCommentTree(comments);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CommentResponse> getLessonComments(UUID courseId, UUID lessonId) {
        Lesson lesson = resolveLesson(courseId, lessonId);
        List<Comment> comments = commentRepository.findAllByTarget(lesson.getId(), CommentTarget.LESSON);
        return buildCommentTree(comments);
    }

    @Override
    public CommentResponse addCourseComment(UUID courseId, CommentRequest request) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new NotFoundException("Course not found"));
        Comment parent = request.getParentId() != null
                ? commentRepository.findByIdAndIsActiveTrue(request.getParentId())
                    .orElseThrow(() -> new NotFoundException("Parent comment not found"))
                : null;

        if (parent != null && (!parent.getTargetId().equals(course.getId()) || parent.getTargetType() != CommentTarget.COURSE)) {
            throw new ForbiddenException("Parent comment belongs to a different course");
        }

        Comment comment = createComment(request, course.getId(), CommentTarget.COURSE, parent);
        commentRepository.save(comment);
        log.debug("Course comment {} created by {}", comment.getId(), comment.getUserId());
        
        // Build response
        CommentResponse response = mapToResponse(comment);
        
        // Broadcast via WebSocket
        CommentWebSocketMessage wsMessage = CommentWebSocketMessage.builder()
                .eventType(CommentEventType.CREATED)
                .commentId(comment.getId())
                .targetId(course.getId())
                .targetType(CommentTargetType.COURSE)
                .userId(comment.getUserId())
                .content(comment.getContent())
                .createdAt(comment.getCreated())
                .build();
        webSocketService.broadcastCommentCreated(wsMessage);
        
        return response;
    }

    @Override
    public CommentResponse addLessonComment(UUID courseId, UUID lessonId, CommentRequest request) {
        Lesson lesson = resolveLesson(courseId, lessonId);
        Comment parent = request.getParentId() != null
                ? commentRepository.findByIdAndIsActiveTrue(request.getParentId())
                    .orElseThrow(() -> new NotFoundException("Parent comment not found"))
                : null;

        if (parent != null && !parent.getTargetId().equals(lesson.getId())) {
            throw new ForbiddenException("Parent comment belongs to a different lesson");
        }

        Comment comment = createComment(request, lesson.getId(), CommentTarget.LESSON, parent);
        commentRepository.save(comment);
        log.debug("Lesson comment {} created by {}", comment.getId(), comment.getUserId());
        
        // Build response
        CommentResponse response = mapToResponse(comment);
        
        // Broadcast via WebSocket
        CommentWebSocketMessage wsMessage = CommentWebSocketMessage.builder()
                .eventType(CommentEventType.CREATED)
                .commentId(comment.getId())
                .targetId(lesson.getId())
                .targetType(CommentTargetType.LESSON)
                .userId(comment.getUserId())
                .content(comment.getContent())
                .createdAt(comment.getCreated())
                .build();
        webSocketService.broadcastCommentCreated(wsMessage);
        
        return response;
    }

    @Override
    @Transactional(readOnly = true)
    public List<CommentResponse> getCodeComments(UUID courseId, UUID lessonId) {
        Lesson lesson = resolveLesson(courseId, lessonId);
        List<Comment> comments = commentRepository.findAllByTarget(lesson.getId(), CommentTarget.CODE);
        return buildCommentTree(comments);
    }

    @Override
    public CommentResponse addCodeComment(UUID courseId, UUID lessonId, CommentRequest request) {
        Lesson lesson = resolveLesson(courseId, lessonId);
        Comment parent = request.getParentId() != null
                ? commentRepository.findByIdAndIsActiveTrue(request.getParentId())
                    .orElseThrow(() -> new NotFoundException("Parent comment not found"))
                : null;

        if (parent != null && (!parent.getTargetId().equals(lesson.getId()) || parent.getTargetType() != CommentTarget.CODE)) {
            throw new ForbiddenException("Parent comment belongs to a different code workspace");
        }

        Comment comment = createComment(request, lesson.getId(), CommentTarget.CODE, parent);
        commentRepository.save(comment);
        log.debug("Code comment {} created by {}", comment.getId(), comment.getUserId());
        return mapToResponse(comment);
    }

    @Override
    public void deleteComment(UUID courseId, UUID commentId) {
        Comment comment = commentRepository.findByIdAndIsActiveTrue(commentId)
                .orElseThrow(() -> new NotFoundException("Comment not found"));

        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new NotFoundException("Course not found"));

        if (comment.getTargetType() == CommentTarget.COURSE && !comment.getTargetId().equals(courseId)) {
            throw new ForbiddenException("Comment does not belong to the specified course");
        }
        if (comment.getTargetType() == CommentTarget.LESSON || comment.getTargetType() == CommentTarget.CODE) {
            Lesson lesson = lessonRepository.findById(comment.getTargetId())
                    .orElseThrow(() -> new NotFoundException("Lesson not found for comment"));
            if (lesson.getChapter() == null || lesson.getChapter().getCourse() == null
                    || !lesson.getChapter().getCourse().getId().equals(courseId)) {
                throw new ForbiddenException("Comment does not belong to the specified course");
            }
        }

        UUID currentUserId = requireCurrentUser();
        boolean isOwner = currentUserId.equals(comment.getUserId());
        boolean isAdmin = UserContext.hasAnyRole("ADMIN");
        boolean isInstructor = UserContext.hasAnyRole("INSTRUCTOR") && course.getInstructorId() != null
                && course.getInstructorId().equals(currentUserId);

        if (!isOwner && !isAdmin && !isInstructor) {
            throw new ForbiddenException("You are not allowed to remove this comment");
        }

        comment.setIsActive(false);
        comment.setUpdatedBy(currentUserId);
        comment.setUpdated(OffsetDateTime.now());
        commentRepository.save(comment);
        log.debug("Comment {} soft-deleted by {}", commentId, currentUserId);
    }

    private Comment createComment(CommentRequest request, UUID targetId, CommentTarget targetType, Comment parent) {
        UUID userId = requireCurrentUser();
        Comment comment = new Comment();
        comment.setContent(request.getContent().trim());
        comment.setUserId(userId);
        comment.setTargetId(targetId);
        comment.setTargetType(targetType);
        comment.setParent(parent);
        comment.setCreatedBy(userId);
        comment.setUpdatedBy(userId);
        comment.setCreated(OffsetDateTime.now());
        comment.setUpdated(OffsetDateTime.now());
        comment.setIsActive(true);
        return comment;
    }

    private List<CommentResponse> buildCommentTree(List<Comment> comments) {
        Map<UUID, CommentResponse> responseMap = new LinkedHashMap<>();
        for (Comment comment : comments) {
            responseMap.put(comment.getId(), mapToResponse(comment));
        }

        List<CommentResponse> roots = new ArrayList<>();
        for (Comment comment : comments) {
            CommentResponse response = responseMap.get(comment.getId());
            Comment parent = comment.getParent();
            if (parent != null && responseMap.containsKey(parent.getId())) {
                responseMap.get(parent.getId()).addReply(response);
            } else {
                roots.add(response);
            }
        }

        return roots;
    }

    private CommentResponse mapToResponse(Comment comment) {
        UUID parentId = comment.getParent() != null ? comment.getParent().getId() : null;
        return CommentResponse.builder()
                .id(comment.getId())
                .parentId(parentId)
                .userId(comment.getUserId())
                .content(comment.getContent())
                .created(comment.getCreated())
                .updated(comment.getUpdated())
                .build();
    }

    private Lesson resolveLesson(UUID courseId, UUID lessonId) {
        Lesson lesson = lessonRepository.findById(lessonId)
                .orElseThrow(() -> new NotFoundException("Lesson not found"));
        if (lesson.getChapter() == null || lesson.getChapter().getCourse() == null) {
            throw new NotFoundException("Lesson is not attached to a course");
        }
        if (!lesson.getChapter().getCourse().getId().equals(courseId)) {
            throw new ForbiddenException("Lesson does not belong to the specified course");
        }
        if (lesson.getIsActive() != null && !lesson.getIsActive()) {
            throw new NotFoundException("Lesson is not active");
        }
        return lesson;
    }

    private UUID requireCurrentUser() {
        UUID userId = UserContext.getCurrentUserId();
        if (userId == null) {
            throw new UnauthorizedException("Authentication required");
        }
        return userId;
    }
}
