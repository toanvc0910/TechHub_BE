package com.techhub.app.aiservice.dto.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * Event published when a lesson is created or updated
 */
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
