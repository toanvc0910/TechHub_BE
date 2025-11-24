package com.techhub.app.userservice.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.Size;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateUserRequest {

    @Size(min = 3, max = 50, message = "Username must be between 3 and 50 characters")
    private String username;

    @Size(max = 500, message = "Avatar URL cannot exceed 500 characters")
    private String avatar;

    private String firstName;
    private String lastName;
    private List<String> roles; // Role names to assign to the user
}
