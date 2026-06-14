package com.techhub.app.courseservice.dto.response;

import com.techhub.app.courseservice.enums.SubmissionStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * A learner's submission enriched with display info, returned to instructors so
 * they can review and grade. One entry represents the learner's latest attempt
 * for a given exercise.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SubmissionResponse {

    private UUID id;
    private UUID userId;
    private String username;
    private String avatar;
    private String answer;
    private Object submissionData;
    private Float grade;
    private String feedback;
    private SubmissionStatus status;
    private OffsetDateTime submittedAt;
    private OffsetDateTime gradedAt;
    private UUID gradedBy;
}
