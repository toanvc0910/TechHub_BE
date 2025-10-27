package com.techhub.app.notificationservice.service;

import com.techhub.app.commonservice.kafka.event.EmailEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailSenderService {

    private final JavaMailSender mailSender;
    private final TemplateEngine templateEngine;

    @Value("${spring.mail.username}")
    private String fromEmail;

    public void sendHtmlEmail(EmailEvent event) throws MessagingException {
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

        helper.setFrom(fromEmail);
        helper.setTo(event.getRecipient());
        helper.setSubject(event.getSubject());

        // Generate HTML content from template
        String htmlContent = generateHtmlContent(event);
        helper.setText(htmlContent, true);

        mailSender.send(message);
        log.info("HTML email sent successfully to: {}", event.getRecipient());
    }

    private String generateHtmlContent(EmailEvent event) {
        String templateName = resolveTemplateName(event.getTemplateCode());

        Context context = new Context();
        if (event.getVariables() != null) {
            event.getVariables().forEach(context::setVariable);
        }

        // Add common variables
        context.setVariable("subject", event.getSubject());
        context.setVariable("currentYear", java.time.Year.now().getValue());

        return templateEngine.process(templateName, context);
    }

    private String resolveTemplateName(String templateCode) {
        if (templateCode == null) {
            return "email/default";
        }

        switch (templateCode) {
            case "otp-verification":
                return "email/otp-verification";
            case "welcome-email":
                return "email/welcome";
            case "password-reset":
                return "email/password-reset";
            case "account-activation":
                return "email/account-activation";
            case "blog-published":
                return "email/blog-published";
            case "blog-commented":
                return "email/blog-commented";
            default:
                return "email/default";
        }
    }
}

