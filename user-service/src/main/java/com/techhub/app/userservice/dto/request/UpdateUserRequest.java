package com.techhub.app.userservice.dto.request;

import lombok.Data;

import javax.validation.constraints.Email;
import javax.validation.constraints.NotBlank;

@Data
public class UpdateUserRequest {

    @Email(message = "Email should be valid")
    private String email;

    private String username;
    private String fullName;
    private String bio;
    private String location;
    private String avatarUrl;
    private String preferredLanguage;
}
