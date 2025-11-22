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
public class LessonEventPayload implements Serializable {

    private static final long serialVersionUID = 1L;

    private String eventType; // CREATED, UPDATED, DELETED
    private String lessonId;
    private String courseId;
    private String chapterId;
    private String title;
    private String content;
    private String contentType;
    private Integer order;
    private Boolean isFree;
    private String videoUrl;
}
