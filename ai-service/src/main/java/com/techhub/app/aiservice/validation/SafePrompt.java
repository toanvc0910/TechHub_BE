package com.techhub.app.aiservice.validation;

import javax.validation.Constraint;
import javax.validation.Payload;
import java.lang.annotation.*;

/**
 * Custom validation annotation to prevent prompt injection attacks
 */
@Documented
@Constraint(validatedBy = SafePromptValidator.class)
@Target({ ElementType.FIELD, ElementType.PARAMETER })
@Retention(RetentionPolicy.RUNTIME)
public @interface SafePrompt {

    String message() default "Message contains prohibited patterns or exceeds safety limits";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
