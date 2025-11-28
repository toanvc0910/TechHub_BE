package com.techhub.app.courseservice.dto.response;

import com.techhub.app.courseservice.enums.CourseLevel;
import com.techhub.app.courseservice.enums.CourseStatus;
import com.techhub.app.courseservice.enums.Language;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CourseSummaryResponse {

    private UUID id;
    private String title;
    private String description;
    private BigDecimal price;
    private BigDecimal discountPrice;
    private OffsetDateTime promoEndDate;
    private CourseStatus status;
    private CourseLevel level;
    private Language language;
    private List<String> categories;
    private List<SkillDTO> skills;
    private List<TagDTO> tags;
    private List<String> objectives;
    private List<String> requirements;
    private UUID instructorId;
    private CourseFileResource thumbnail;
    private CourseFileResource introVideo;
    private OffsetDateTime created;
    private OffsetDateTime updated;
    private Boolean active;
    private long totalEnrollments;
    private Double averageRating;
    private Long ratingCount;
}
