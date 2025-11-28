package com.techhub.app.aiservice.dto.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * Event published when a course is created, updated, or deleted
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CourseEvent {

    private EventType eventType;
    private UUID courseId;
    private String title;
    private String description;
    private String objectives;
    private String requirements;
    private String categories;
    private String tags;
    private String level;
    private String language;
    private UUID instructorId;
    private String status;

    public enum EventType {
        CREATED,
        UPDATED,
        DELETED,
        PUBLISHED
    }
}
