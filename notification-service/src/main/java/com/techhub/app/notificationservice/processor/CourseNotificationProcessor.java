package com.techhub.app.notificationservice.processor;

import com.techhub.app.commonservice.kafka.event.notification.NotificationCommand;
import com.techhub.app.commonservice.kafka.event.notification.NotificationRecipient;
import com.techhub.app.commonservice.kafka.event.notification.NotificationType;
import com.techhub.app.notificationservice.entity.Notification;
import com.techhub.app.notificationservice.service.NotificationDeliveryService;
import com.techhub.app.notificationservice.service.NotificationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.Set;

/**
 * Processor for course-related notifications:
 * - NEW_COURSE: Broadcast to all users about new courses
 * - NEW_LESSON: Notify enrolled students about new lessons
 * - NEW_CONTENT: Notify enrolled students about new content (assets, exercises)
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
@Slf4j
public class CourseNotificationProcessor extends AbstractNotificationProcessor {

    private static final Set<NotificationType> SUPPORTED_TYPES = Set.of(
            NotificationType.NEW_COURSE,
            NotificationType.NEW_LESSON,
            NotificationType.NEW_CONTENT);

    private static final String DEFAULT_COURSE_TITLE = "Course Update";
    private static final String DEFAULT_LESSON_TITLE = "New Lesson Available";
    private static final String DEFAULT_CONTENT_TITLE = "New Content Available";

    public CourseNotificationProcessor(NotificationService notificationService,
            NotificationDeliveryService notificationDeliveryService) {
        super(notificationService, notificationDeliveryService);
    }

    @Override
    public boolean supports(NotificationCommand command) {
        return command != null && command.getType() != null && SUPPORTED_TYPES.contains(command.getType());
    }

    @Override
    public void process(NotificationCommand command) {
        log.info("📚 Processing course notification: type={}, commandId={}, recipients={}",
                command.getType(), command.getCommandId(),
                command.getRecipients() != null ? command.getRecipients().size() : 0);

        // Dispatch to all recipients
        dispatch(command);
    }

    @Override
    protected Notification buildNotification(NotificationCommand command, NotificationRecipient recipient) {
        Notification notification = super.buildNotification(command, recipient);

        if (!StringUtils.hasText(notification.getTitle())) {
            notification.setTitle(getDefaultTitle(command.getType()));
        }

        return notification;
    }

    @Override
    protected String buildFallbackMessage(NotificationCommand command, NotificationRecipient recipient) {
        NotificationType type = command.getType();

        if (type == NotificationType.NEW_COURSE) {
            return "A new course is available on TechHub. Check it out!";
        } else if (type == NotificationType.NEW_LESSON) {
            return "A new lesson has been added to your enrolled course.";
        } else if (type == NotificationType.NEW_CONTENT) {
            return "New content has been added to your enrolled course.";
        }

        return "There's an update to your course.";
    }

    private String getDefaultTitle(NotificationType type) {
        if (type == NotificationType.NEW_COURSE) {
            return DEFAULT_COURSE_TITLE;
        } else if (type == NotificationType.NEW_LESSON) {
            return DEFAULT_LESSON_TITLE;
        } else if (type == NotificationType.NEW_CONTENT) {
            return DEFAULT_CONTENT_TITLE;
        }
        return DEFAULT_COURSE_TITLE;
    }
}
