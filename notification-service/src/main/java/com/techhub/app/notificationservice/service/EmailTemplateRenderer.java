package com.techhub.app.notificationservice.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import org.thymeleaf.context.Context;
import org.thymeleaf.exceptions.TemplateInputException;
import org.thymeleaf.spring5.SpringTemplateEngine;

import java.util.Map;
import java.util.Optional;

@Component
@Slf4j
public class EmailTemplateRenderer {

    private final SpringTemplateEngine templateEngine;

    public EmailTemplateRenderer(@Qualifier("emailTemplateEngine") SpringTemplateEngine templateEngine) {
        this.templateEngine = templateEngine;
    }

    public Optional<String> render(String templateCode, Map<String, Object> variables) {
        if (!StringUtils.hasText(templateCode)) {
            return Optional.empty();
        }
        Context context = new Context();
        if (!CollectionUtils.isEmpty(variables)) {
            context.setVariables(variables);
        }
        try {
            return Optional.of(templateEngine.process(templateCode, context));
        } catch (TemplateInputException ex) {
            log.warn("Email template '{}' could not be processed: {}", templateCode, ex.getMessage());
            return Optional.empty();
        } catch (Exception ex) {
            log.error("Failed to render email template '{}'", templateCode, ex);
            return Optional.empty();
        }
    }
}
