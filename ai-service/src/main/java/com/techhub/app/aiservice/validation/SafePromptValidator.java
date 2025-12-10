package com.techhub.app.aiservice.validation;

import com.techhub.app.aiservice.service.PromptSanitizationService;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

/**
 * Validator for prompt injection prevention
 */
public class SafePromptValidator implements ConstraintValidator<SafePrompt, String> {

    private final PromptSanitizationService sanitizationService;

    public SafePromptValidator() {
        this.sanitizationService = new PromptSanitizationService();
    }

    @Override
    public void initialize(SafePrompt constraintAnnotation) {
        // No initialization needed
    }

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null || value.trim().isEmpty()) {
            return false;
        }

        // Use sanitization service to validate
        boolean valid = sanitizationService.isValid(value);

        if (!valid) {
            // Get specific violation reason and add to context
            String reason = sanitizationService.getViolationReason(value);
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate(reason)
                    .addConstraintViolation();
        }

        return valid;
    }
}
