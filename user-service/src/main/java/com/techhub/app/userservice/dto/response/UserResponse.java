package com.techhub.app.userservice.dto.response;

import com.techhub.app.userservice.enums.Language;
import com.techhub.app.userservice.enums.UserRole;
import com.techhub.app.userservice.enums.UserStatus;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
public class UserResponse {

    private UUID id;
    private String email;
    private String username;
    private UserRole role;
    private UserStatus status;
    private LocalDateTime created;
    private LocalDateTime updated;

    // Profile information
    private String fullName;
    private String avatarUrl;
    private String bio;
    private String location;
    private Language preferredLanguage;
}
