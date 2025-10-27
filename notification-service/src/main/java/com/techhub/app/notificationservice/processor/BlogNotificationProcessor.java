package com.techhub.app.notificationservice.processor;

import com.techhub.app.commonservice.kafka.event.notification.NotificationCommand;
import com.techhub.app.commonservice.kafka.event.notification.NotificationRecipient;
import com.techhub.app.commonservice.kafka.event.notification.NotificationType;
import com.techhub.app.notificationservice.entity.Notification;
import com.techhub.app.notificationservice.service.NotificationDeliveryService;
import com.techhub.app.notificationservice.service.NotificationService;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.Map;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
public class BlogNotificationProcessor extends AbstractNotificationProcessor {

    private static final String DEFAULT_TITLE = "Blog update";

    public BlogNotificationProcessor(NotificationService notificationService,
                                     NotificationDeliveryService notificationDeliveryService) {
        super(notificationService, notificationDeliveryService);
    }

    @Override
    public boolean supports(NotificationCommand command) {
        return command != null && command.getType() == NotificationType.BLOG;
    }

    @Override
    public void process(NotificationCommand command) {
        dispatch(command);
    }

    @Override
    protected String buildFallbackMessage(NotificationCommand command, NotificationRecipient recipient) {
        String blogTitle = extractBlogTitle(command.getMetadata());
        if (StringUtils.hasText(blogTitle)) {
            return "Blog \"" + blogTitle + "\" has a new update.";
        }
        return "A blog you are following has a new update.";
    }

    @Override
    protected Notification buildNotification(NotificationCommand command, NotificationRecipient recipient) {
        Notification notification = super.buildNotification(command, recipient);
        if (!StringUtils.hasText(notification.getTitle())) {
            notification.setTitle(DEFAULT_TITLE);
        }
        return notification;
    }

    private String extractBlogTitle(Map<String, Object> metadata) {
        if (metadata == null) {
            return null;
        }
        Object title = metadata.get("blogTitle");
        return title != null ? title.toString() : null;
    }
}
