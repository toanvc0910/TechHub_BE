package com.techhub.app.notificationservice.service;

import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailSender {

    private final JavaMailSender mailSender;
    private final EmailTemplateRenderer templateRenderer;

    @Value("${app.email.enabled:true}")
    private boolean emailEnabled;

    @Value("${app.email.from:no-reply@techhub.app}")
    private String fromAddress;

    @Value("${app.email.reply-to:}")
    private String replyToAddress;

    public void send(String recipient,
                     String subject,
                     String templateCode,
                     Map<String, Object> variables,
                     String fallbackBody) {
        if (!emailEnabled) {
            log.info("Email delivery disabled. Skipping email '{}' using template '{}'", recipient, templateCode);
            return;
        }
        if (!StringUtils.hasText(recipient)) {
            log.warn("Skip email delivery due to missing recipient");
            return;
        }

        String plainBody = StringUtils.hasText(fallbackBody) ? fallbackBody : subject;

        Optional<String> renderedTemplate = templateRenderer.render(templateCode, variables);
        boolean deliveredWithHtml = renderedTemplate
                .map(rendered -> dispatch(recipient, subject, rendered, true))
                .orElseGet(() -> dispatch(recipient, subject, plainBody, false));

        if (!deliveredWithHtml && renderedTemplate.isPresent()) {
            dispatch(recipient, subject, plainBody, false);
        }

        log.debug("Email delivery for '{}' with template '{}' completed. HTML template attempted: {}",
                recipient, templateCode, deliveredWithHtml);
    }

    private boolean dispatch(String recipient,
                             String subject,
                             String body,
                             boolean html) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, MimeMessageHelper.MULTIPART_MODE_MIXED_RELATED,
                    StandardCharsets.UTF_8.name());

            helper.setTo(recipient);
            helper.setSubject(subject);
            helper.setText(body, html);
            helper.setFrom(fromAddress);
            if (StringUtils.hasText(replyToAddress)) {
                helper.setReplyTo(replyToAddress);
            }

            mailSender.send(message);
            return html;
        } catch (MessagingException ex) {
            log.error("Failed to send email to {}", recipient, ex);
            return false;
        }
    }
}
