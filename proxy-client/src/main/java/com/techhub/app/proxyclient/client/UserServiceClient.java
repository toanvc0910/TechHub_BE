package com.techhub.app.proxyclient.client;

import com.techhub.app.proxyclient.client.dto.ApiResponse;
import com.techhub.app.proxyclient.client.dto.CreateUserRequest;
import com.techhub.app.proxyclient.client.dto.UserResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class UserServiceClient {

    private final RestTemplate restTemplate;

    @Value("${client.userservice.base-url:http://localhost:8700}")
    private String baseUrl;

    public UserResponse getUserById(UUID id) {
        String url = baseUrl + "/api/users/" + id;
        ResponseEntity<ApiResponse> resp = restTemplate.getForEntity(url, ApiResponse.class);
        ApiResponse body = resp.getBody();
        if (body == null || !body.isSuccess()) {
            throw new RuntimeException("UserService getUserById failed: " + (body != null ? body.getMessage() : "no body"));
        }
        return body.getData(UserResponse.class);
    }

    public UserResponse getUserByEmail(String email) {
        String url = baseUrl + "/api/users/email/" + email;
        ResponseEntity<ApiResponse> resp = restTemplate.getForEntity(url, ApiResponse.class);
        ApiResponse body = resp.getBody();
        if (body == null || !body.isSuccess()) {
            throw new RuntimeException("UserService getUserByEmail failed: " + (body != null ? body.getMessage() : "no body"));
        }
        return body.getData(UserResponse.class);
    }

    public UserResponse createUser(CreateUserRequest request) {
        String url = baseUrl + "/api/users";
        ResponseEntity<ApiResponse> resp = restTemplate.exchange(url, HttpMethod.POST, new HttpEntity<>(request), ApiResponse.class);
        ApiResponse body = resp.getBody();
        if (body == null || !body.isSuccess()) {
            throw new RuntimeException("UserService createUser failed: " + (body != null ? body.getMessage() : "no body"));
        }
        return body.getData(UserResponse.class);
    }
}

