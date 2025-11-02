package com.techhub.app.courseservice.controller;

import com.techhub.app.courseservice.dto.CreateUserCodeDTO;
import com.techhub.app.courseservice.dto.UserCodeDTO;
import com.techhub.app.courseservice.service.UserCodeService;
import com.techhub.app.courseservice.utils.ResponseWrapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import javax.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/user-codes")
@Tag(name = "User Code Management", description = "APIs for managing user code snippets")
public class UserCodeController {

    private final UserCodeService userCodeService;

    public UserCodeController(UserCodeService userCodeService) {
        this.userCodeService = userCodeService;
    }

    @PostMapping
    @Operation(summary = "Save user code")
    public ResponseEntity<ResponseWrapper<UserCodeDTO>> saveUserCode(@Valid @RequestBody CreateUserCodeDTO createUserCodeDTO) {
        UserCodeDTO userCodeDTO = userCodeService.saveUserCode(createUserCodeDTO);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ResponseWrapper.success(userCodeDTO, "User code saved successfully"));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get user code by ID")
    public ResponseEntity<ResponseWrapper<UserCodeDTO>> getUserCodeById(@PathVariable UUID id) {
        UserCodeDTO userCodeDTO = userCodeService.getUserCodeById(id);
        return ResponseEntity.ok(ResponseWrapper.success(userCodeDTO));
    }

    @GetMapping("/user/{userId}")
    @Operation(summary = "Get user codes by user")
    public ResponseEntity<ResponseWrapper<List<UserCodeDTO>>> getUserCodesByUser(@PathVariable UUID userId) {
        List<UserCodeDTO> userCodes = userCodeService.getUserCodesByUser(userId);
        return ResponseEntity.ok(ResponseWrapper.success(userCodes));
    }

    @GetMapping("/lesson/{lessonId}")
    @Operation(summary = "Get user codes by lesson")
    public ResponseEntity<ResponseWrapper<List<UserCodeDTO>>> getUserCodesByLesson(@PathVariable UUID lessonId) {
        List<UserCodeDTO> userCodes = userCodeService.getUserCodesByLesson(lessonId);
        return ResponseEntity.ok(ResponseWrapper.success(userCodes));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete user code")
    public ResponseEntity<ResponseWrapper<Void>> deleteUserCode(@PathVariable UUID id) {
        userCodeService.deleteUserCode(id);
        return ResponseEntity.ok(ResponseWrapper.success(null, "User code deleted successfully"));
    }
}
