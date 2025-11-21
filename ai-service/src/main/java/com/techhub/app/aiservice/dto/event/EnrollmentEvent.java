package com.techhub.app.aiservice.dto.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * Event published when a user enrolls in a course or updates progress
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EnrollmentEvent {

    private EventType eventType;
    private UUID enrollmentId;
    private UUID userId;
    private UUID courseId;
    private String status;
    private Double progressPercentage;
    private Integer completedLessons;
    private Integer totalLessons;

    public enum EventType {
        ENROLLED,
        PROGRESS_UPDATED,
        COMPLETED,
        DROPPED
    }
}
