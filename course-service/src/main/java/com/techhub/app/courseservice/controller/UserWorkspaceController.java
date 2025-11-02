package com.techhub.app.courseservice.controller;

import com.techhub.app.commonservice.payload.GlobalResponse;
import com.techhub.app.courseservice.dto.request.UserCodeRequest;
import com.techhub.app.courseservice.dto.response.UserCodeResponse;
import com.techhub.app.courseservice.service.UserWorkspaceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import java.util.UUID;

@RestController
@RequestMapping("/api/courses/{courseId}/lessons/{lessonId}/workspace")
@RequiredArgsConstructor
@Slf4j
@Validated
public class UserWorkspaceController {

    private final UserWorkspaceService userWorkspaceService;

    @GetMapping
    public ResponseEntity<GlobalResponse<UserCodeResponse>> getWorkspace(@PathVariable UUID courseId,
                                                                         @PathVariable UUID lessonId,
                                                                         HttpServletRequest request) {
        UserCodeResponse response = userWorkspaceService.getUserCode(courseId, lessonId);
        return ResponseEntity.ok(
                GlobalResponse.success("Workspace retrieved", response)
                        .withPath(request.getRequestURI())
        );
    }

    @PutMapping
    public ResponseEntity<GlobalResponse<UserCodeResponse>> saveWorkspace(@PathVariable UUID courseId,
                                                                          @PathVariable UUID lessonId,
                                                                          @Valid @RequestBody UserCodeRequest codeRequest,
                                                                          HttpServletRequest request) {
        UserCodeResponse response = userWorkspaceService.saveUserCode(courseId, lessonId, codeRequest);
        return ResponseEntity.ok(
                GlobalResponse.success("Workspace saved", response)
                        .withStatus("WORKSPACE_SAVED")
                        .withPath(request.getRequestURI())
        );
    }
}
