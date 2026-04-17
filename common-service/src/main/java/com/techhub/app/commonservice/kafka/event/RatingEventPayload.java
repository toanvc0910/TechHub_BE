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
public class RatingEventPayload implements Serializable {

    private static final long serialVersionUID = 1L;

    private String eventType; // CREATED, UPDATED, DELETED
    private String ratingId;
    private String userId;
    private String courseId;
    private Integer score; // 1-5
    private String comment;
}
