package com.techhub.app.userservice.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserResponse {

    private UUID id;
    private String email;
    private String username;
    private List<String> roles;
    private String status;
    private LocalDateTime created;
    private LocalDateTime updated;
    private Boolean isActive;
}
