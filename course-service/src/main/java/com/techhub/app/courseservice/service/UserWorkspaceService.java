package com.techhub.app.courseservice.service;

import com.techhub.app.courseservice.dto.request.UserCodeRequest;
import com.techhub.app.courseservice.dto.response.UserCodeResponse;

import java.util.UUID;

public interface UserWorkspaceService {

    UserCodeResponse getUserCode(UUID courseId, UUID lessonId);

    UserCodeResponse saveUserCode(UUID courseId, UUID lessonId, UserCodeRequest request);
}
