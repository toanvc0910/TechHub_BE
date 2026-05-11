package com.techhub.app.courseservice.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class QuizFeedbackResponse {

    private Boolean correct;
    private String summary;
    private String explanation;
    private List<String> selectedAnswers;
    private List<String> correctAnswers;
    private List<String> weakConcepts;
    private List<ReviewSuggestionResponse> reviewSuggestions;
    private String nextAction;
    private String source;
}
