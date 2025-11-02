package com.techhub.app.courseservice.dto.response;

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
public class CourseRatingResponse {

    private UUID courseId;
    private Double averageRating;
    private Long ratingCount;
    private Integer userScore;
}
