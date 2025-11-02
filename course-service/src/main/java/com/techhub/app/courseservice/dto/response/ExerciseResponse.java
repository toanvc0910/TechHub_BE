package com.techhub.app.courseservice.dto.response;

import com.techhub.app.courseservice.dto.ExerciseTestCaseDto;
import com.techhub.app.courseservice.enums.ExerciseType;
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
public class ExerciseResponse {

    private UUID id;
    private ExerciseType type;
    private String question;
    private Object options;
    private List<ExerciseTestCaseDto> testCases;
    private SubmissionStatus lastSubmissionStatus;
    private Float bestScore;
    private OffsetDateTime lastSubmittedAt;
}
