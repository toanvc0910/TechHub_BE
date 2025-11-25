package com.techhub.app.courseservice.dto.response;

import com.techhub.app.courseservice.enums.EnrollmentStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EnrollmentResponse {
    
    private UUID id;
    private UUID userId;
    private UUID courseId;
    private String courseName;
    private EnrollmentStatus status;
    private OffsetDateTime enrolledAt;
    private OffsetDateTime completedAt;
    private Boolean isActive;
}

