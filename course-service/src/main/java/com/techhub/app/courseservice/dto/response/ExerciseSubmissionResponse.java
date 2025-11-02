package com.techhub.app.courseservice.dto.response;

import com.techhub.app.courseservice.enums.SubmissionStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ExerciseSubmissionResponse {

    private UUID submissionId;
    private SubmissionStatus status;
    private Float grade;
    private OffsetDateTime gradedAt;
    private Boolean passed;
    private List<TestCaseResultResponse> testCaseResults;
}
