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
public class LearningPathEventPayload implements Serializable {

    private static final long serialVersionUID = 1L;

    private String eventType; // CREATED, UPDATED, DELETED, COURSE_ADDED, COURSE_REMOVED, REORDERED
    private String pathId;
    private String title;
    private String description;
    private Integer courseCount;
}
