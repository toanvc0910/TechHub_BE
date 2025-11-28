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

import java.util.Map;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
@Slf4j
public class BlogNotificationProcessor extends AbstractNotificationProcessor {

    private static final String DEFAULT_TITLE = "Blog update";

    public BlogNotificationProcessor(NotificationService notificationService,
            NotificationDeliveryService notificationDeliveryService) {
        super(notificationService, notificationDeliveryService);
    }

    @Override
    public boolean supports(NotificationCommand command) {
        boolean supports = command != null && command.getType() == NotificationType.BLOG;
        log.debug("📝 [BLOG PROCESSOR] supports() check: type={}, result={}",
                command != null ? command.getType() : "null", supports);
        return supports;
    }

    @Override
    public void process(NotificationCommand command) {
        log.info("📝 [BLOG PROCESSOR] ===== PROCESSING BLOG NOTIFICATION =====");
        log.info("📝 [BLOG PROCESSOR] CommandId: {}, Title: {}", command.getCommandId(), command.getTitle());
        log.info("📝 [BLOG PROCESSOR] Recipients count: {}",
                command.getRecipients() != null ? command.getRecipients().size() : 0);
        log.info("📝 [BLOG PROCESSOR] DeliveryMethods: {}", command.getDeliveryMethods());
        log.info("📝 [BLOG PROCESSOR] Metadata: {}", command.getMetadata());

        dispatch(command);

        log.info("📝 [BLOG PROCESSOR] ✅ Blog notification dispatched successfully");
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
