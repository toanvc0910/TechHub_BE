package com.techhub.app.courseservice.dto.response;

import com.techhub.app.courseservice.enums.TestCaseVisibility;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TestCaseResultResponse {

    private UUID testCaseId;
    private boolean passed;
    private String input;
    private String expectedOutput;
    private String actualOutput;
    private TestCaseVisibility visibility;
    private Float weight;
}
