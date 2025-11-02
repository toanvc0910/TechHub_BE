package com.techhub.app.courseservice.dto.response;

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
public class ChapterResponse {

    private UUID id;
    private String title;
    private Integer orderIndex;
    private Float minCompletionThreshold;
    private Boolean autoUnlock;
    private Boolean locked;
    private Boolean unlocked;
    private Double completionRatio;
    private Boolean currentChapter;
    private OffsetDateTime created;
    private OffsetDateTime updated;
    private List<LessonResponse> lessons;
}
