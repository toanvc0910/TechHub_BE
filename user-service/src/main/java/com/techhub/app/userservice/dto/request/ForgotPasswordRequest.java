package com.techhub.app.userservice.dto.request;

import lombok.Data;

import javax.validation.constraints.Email;
import javax.validation.constraints.NotBlank;

@Data
public class ForgotPasswordRequest {

    @Email(message = "Email should be valid")
    @NotBlank(message = "Email is required")
    private String email;
}
