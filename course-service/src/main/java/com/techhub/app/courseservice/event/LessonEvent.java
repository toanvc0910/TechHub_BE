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
public class LessonEvent {

    private EventType eventType;
    private UUID lessonId;
    private UUID courseId;
    private UUID chapterId;
    private String title;
    private String content;
    private String contentType;
    private Integer order;
    private Boolean isFree;

    public enum EventType {
        CREATED,
        UPDATED,
        DELETED
    }
}
