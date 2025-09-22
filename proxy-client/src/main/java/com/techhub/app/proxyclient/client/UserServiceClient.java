package com.techhub.app.proxyclient.client;

import com.techhub.app.proxyclient.client.dto.ApiResponse;
import com.techhub.app.proxyclient.client.dto.CreateUserRequest;
import com.techhub.app.proxyclient.client.dto.UserResponse;
import com.techhub.app.proxyclient.config.FeignConfig;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.UUID;

@FeignClient(
    name = "user-service",
    url = "${services.user-service.url:http://localhost:8700}",
    configuration = FeignConfig.class
)
public interface UserServiceClient {

    @PostMapping("/api/users")
    ApiResponse<UserResponse> createUser(@Valid @RequestBody CreateUserRequest request);

    @GetMapping("/api/users/{userId}")
    ApiResponse<UserResponse> getUserById(@PathVariable UUID userId);

    @GetMapping("/api/users/email/{email}")
    ApiResponse<UserResponse> getUserByEmail(@PathVariable String email);

    @GetMapping("/api/users/username/{username}")
    ApiResponse<UserResponse> getUserByUsername(@PathVariable String username);

    @GetMapping("/api/users")
    ApiResponse<Page<UserResponse>> getAllUsers(@RequestParam int page, @RequestParam int size);

    @GetMapping("/api/users/search")
    ApiResponse<Page<UserResponse>> searchUsers(
        @RequestParam String keyword,
        @RequestParam int page,
        @RequestParam int size
    );

    @GetMapping("/api/users/exists/email/{email}")
    ApiResponse<Boolean> checkEmailExists(@PathVariable String email);

    @GetMapping("/api/users/exists/username/{username}")
    ApiResponse<Boolean> checkUsernameExists(@PathVariable String username);
}
