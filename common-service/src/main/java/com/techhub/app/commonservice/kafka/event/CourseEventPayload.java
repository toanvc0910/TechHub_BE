package com.techhub.app.commonservice.kafka.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CourseEventPayload implements Serializable {

    private static final long serialVersionUID = 1L;

    private String eventType; // CREATED, UPDATED, DELETED, PUBLISHED
    private String courseId;
    private String title;
    private String description;
    private String objectives;
    private String requirements;
    private String categories;
    private String tags;
    private String level;
    private String language;
    private String instructorId;
    private String status;
}
