package com.techhub.app.courseservice.dto.request;

import lombok.Getter;
import lombok.Setter;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

@Getter
@Setter
public class ChapterRequest {

    @NotBlank
    @Size(max = 255)
    private String title;

    private Integer orderIndex;

    private Float minCompletionThreshold;

    private Boolean autoUnlock;

    private Boolean locked;
}
