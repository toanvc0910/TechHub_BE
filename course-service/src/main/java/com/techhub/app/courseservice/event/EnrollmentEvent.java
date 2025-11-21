package com.techhub.app.courseservice.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

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
