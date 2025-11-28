package com.techhub.app.courseservice.service.impl;

import com.techhub.app.commonservice.kafka.event.notification.NotificationCommand;
import com.techhub.app.commonservice.kafka.event.notification.NotificationDeliveryMethod;
import com.techhub.app.commonservice.kafka.event.notification.NotificationRecipient;
import com.techhub.app.commonservice.kafka.event.notification.NotificationType;
import com.techhub.app.commonservice.kafka.publisher.NotificationCommandPublisher;
import com.techhub.app.courseservice.client.UserServiceClient;
import com.techhub.app.courseservice.entity.Enrollment;
import com.techhub.app.courseservice.repository.EnrollmentRepository;
import com.techhub.app.courseservice.service.CourseNotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class CourseNotificationServiceImpl implements CourseNotificationService {

    private final NotificationCommandPublisher notificationCommandPublisher;
    private final EnrollmentRepository enrollmentRepository;
    private final UserServiceClient userServiceClient;

    @Override
    @Async
    public void notifyNewCourse(UUID courseId, String courseTitle) {
        log.info("📢 ===== START notifyNewCourse =====");
        log.info("📢 Notifying all users about new course: courseId={}, title={}", courseId, courseTitle);

        // Get all active user IDs from user-service
        List<UUID> allUserIds = getAllActiveUserIds();
        log.info("📢 Retrieved {} active user IDs", allUserIds != null ? allUserIds.size() : 0);

        if (CollectionUtils.isEmpty(allUserIds)) {
            log.info("📢 No active users found, skipping new course notification");
            return;
        }

        String title = "New Course Available!";
        String message = String.format("A new course \"%s\" is now available on TechHub. Start learning today!",
                courseTitle);

        Map<String, Object> metadata = new HashMap<>();
        metadata.put("source", "course-service");
        metadata.put("action", "new-course");
        metadata.put("courseId", courseId.toString());
        metadata.put("courseTitle", courseTitle);

        // Send notification to all users
        List<NotificationRecipient> recipients = allUserIds.stream()
                .map(userId -> NotificationRecipient.builder()
                        .userId(userId)
                        .build())
                .collect(Collectors.toList());

        log.info("📢 Created {} recipients for notification", recipients.size());

        NotificationCommand command = NotificationCommand.builder()
                .type(NotificationType.NEW_COURSE)
                .title(title)
                .message(message)
                .templateCode("new-course")
                .templateVariables(Map.of(
                        "courseTitle", courseTitle,
                        "courseId", courseId.toString()))
                .deliveryMethods(EnumSet.of(NotificationDeliveryMethod.IN_APP))
                .recipients(recipients)
                .metadata(metadata)
                .build();

        log.info("📢 Publishing notification command: type={}, recipients={}", command.getType(), recipients.size());
        notificationCommandPublisher.publish(command);
        log.info("📢 ✅ New course notification published to {} users for courseId={}", allUserIds.size(), courseId);
        log.info("📢 ===== END notifyNewCourse =====");
    }

    @Override
    @Async
    public void notifyNewLesson(UUID courseId, String courseTitle, UUID lessonId, String lessonTitle) {
        log.info("📚 Notifying enrolled students about new lesson: courseId={}, lessonId={}", courseId, lessonId);

        List<UUID> enrolledUserIds = getEnrolledUserIds(courseId);
        if (CollectionUtils.isEmpty(enrolledUserIds)) {
            log.info("📚 No enrolled students for courseId={}, skipping notification", courseId);
            return;
        }

        String title = "New Lesson Available!";
        String message = String.format("A new lesson \"%s\" has been added to \"%s\". Continue your learning!",
                lessonTitle, courseTitle);

        publishToEnrolledStudents(
                NotificationType.PROGRESS,
                enrolledUserIds,
                title,
                message,
                "new-lesson",
                Map.of(
                        "courseId", courseId.toString(),
                        "courseTitle", courseTitle,
                        "lessonId", lessonId.toString(),
                        "lessonTitle", lessonTitle),
                createMetadata("new-lesson", courseId, lessonId, null));

        log.info("📚 New lesson notification sent to {} enrolled students", enrolledUserIds.size());
    }

    @Override
    @Async
    public void notifyNewContent(UUID courseId, String courseTitle, UUID lessonId, String lessonTitle,
            String assetType) {
        log.info("📄 Notifying enrolled students about new content: courseId={}, lessonId={}, type={}",
                courseId, lessonId, assetType);

        List<UUID> enrolledUserIds = getEnrolledUserIds(courseId);
        if (CollectionUtils.isEmpty(enrolledUserIds)) {
            log.info("📄 No enrolled students for courseId={}, skipping notification", courseId);
            return;
        }

        String title = "New Content Added!";
        String message = String.format("New %s has been added to lesson \"%s\" in course \"%s\".",
                assetType.toLowerCase(), lessonTitle, courseTitle);

        publishToEnrolledStudents(
                NotificationType.PROGRESS,
                enrolledUserIds,
                title,
                message,
                "new-content",
                Map.of(
                        "courseId", courseId.toString(),
                        "courseTitle", courseTitle,
                        "lessonId", lessonId.toString(),
                        "lessonTitle", lessonTitle,
                        "assetType", assetType),
                createMetadata("new-content", courseId, lessonId, assetType));

        log.info("📄 New content notification sent to {} enrolled students", enrolledUserIds.size());
    }

    @Override
    @Async
    public void notifyNewExercise(UUID courseId, String courseTitle, UUID lessonId, String lessonTitle) {
        log.info("🏋️ Notifying enrolled students about new exercise: courseId={}, lessonId={}", courseId, lessonId);

        List<UUID> enrolledUserIds = getEnrolledUserIds(courseId);
        if (CollectionUtils.isEmpty(enrolledUserIds)) {
            log.info("🏋️ No enrolled students for courseId={}, skipping notification", courseId);
            return;
        }

        String title = "New Exercise Available!";
        String message = String.format(
                "A new exercise has been added to lesson \"%s\" in course \"%s\". Test your skills!",
                lessonTitle, courseTitle);

        publishToEnrolledStudents(
                NotificationType.PROGRESS,
                enrolledUserIds,
                title,
                message,
                "new-exercise",
                Map.of(
                        "courseId", courseId.toString(),
                        "courseTitle", courseTitle,
                        "lessonId", lessonId.toString(),
                        "lessonTitle", lessonTitle),
                createMetadata("new-exercise", courseId, lessonId, "EXERCISE"));

        log.info("🏋️ New exercise notification sent to {} enrolled students", enrolledUserIds.size());
    }

    private List<UUID> getEnrolledUserIds(UUID courseId) {
        return enrollmentRepository.findAllByCourse_IdAndIsActiveTrue(courseId)
                .stream()
                .map(Enrollment::getUserId)
                .distinct()
                .collect(Collectors.toList());
    }

    @SuppressWarnings("unchecked")
    private List<UUID> getAllActiveUserIds() {
        log.info("🔍 [DEBUG] Fetching all active user IDs from user-service...");
        try {
            Map<String, Object> response = userServiceClient.getAllActiveUserIds();
            log.info("🔍 [DEBUG] Response from user-service: {}", response);

            if (response != null && response.containsKey("data")) {
                List<String> userIdStrings = (List<String>) response.get("data");
                log.info("🔍 [DEBUG] Found {} user IDs: {}", userIdStrings.size(), userIdStrings);

                List<UUID> uuids = userIdStrings.stream()
                        .map(UUID::fromString)
                        .collect(Collectors.toList());
                log.info("🔍 [DEBUG] Converted to {} UUIDs", uuids.size());
                return uuids;
            }
            log.warn("🔍 [DEBUG] Empty or invalid response from user-service for getAllActiveUserIds");
            return List.of();
        } catch (Exception e) {
            log.error("🔍 [DEBUG] Failed to fetch all active user IDs from user-service: {}", e.getMessage(), e);
            return List.of();
        }
    }

    private void publishToEnrolledStudents(NotificationType type,
            List<UUID> userIds,
            String title,
            String message,
            String templateCode,
            Map<String, Object> templateVariables,
            Map<String, Object> metadata) {
        List<NotificationRecipient> recipients = userIds.stream()
                .map(userId -> NotificationRecipient.builder()
                        .userId(userId)
                        .build())
                .collect(Collectors.toList());

        NotificationCommand command = NotificationCommand.builder()
                .type(type)
                .title(title)
                .message(message)
                .templateCode(templateCode)
                .templateVariables(templateVariables)
                .deliveryMethods(EnumSet.of(NotificationDeliveryMethod.IN_APP))
                .recipients(recipients)
                .metadata(metadata)
                .build();

        notificationCommandPublisher.publish(command);
    }

    private Map<String, Object> createMetadata(String action, UUID courseId, UUID lessonId, String assetType) {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("source", "course-service");
        metadata.put("action", action);
        if (courseId != null) {
            metadata.put("courseId", courseId.toString());
        }
        if (lessonId != null) {
            metadata.put("lessonId", lessonId.toString());
        }
        if (assetType != null) {
            metadata.put("assetType", assetType);
        }
        return metadata;
    }
}
