package com.techhub.app.proxyclient.client.dto;

import lombok.Data;

@Data
public class CreateUserRequest {
    private String email;
    private String username;
    private String password;
    private String fullName;
}

