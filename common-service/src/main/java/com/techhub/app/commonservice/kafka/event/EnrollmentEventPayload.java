package com.techhub.app.commonservice.kafka.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EnrollmentEventPayload implements Serializable {

    private static final long serialVersionUID = 1L;

    private String eventType; // ENROLLED, PROGRESS_UPDATED, COMPLETED, DROPPED
    private String enrollmentId;
    private String userId;
    private String courseId;
    private String status;
    private Double progressPercentage;
    private Integer completedLessons;
    private Integer totalLessons;
}
