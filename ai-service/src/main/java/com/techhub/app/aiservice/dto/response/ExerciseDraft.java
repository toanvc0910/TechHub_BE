package com.techhub.app.aiservice.dto.response;

import com.techhub.app.aiservice.enums.ExerciseFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExerciseDraft {
    private ExerciseFormat type;
    private String question;
    private List<String> options;
    private List<TestCase> testCases;
    private String explanation;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TestCase {
        private String input;
        private String expectedOutput;
        private boolean hidden;
    }
}
