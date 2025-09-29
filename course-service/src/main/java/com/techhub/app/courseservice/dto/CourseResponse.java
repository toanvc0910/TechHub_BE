package com.techhub.app.courseservice.dto;

import com.techhub.app.courseservice.enums.CourseStatus;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
public class CourseResponse {
    private UUID id;
    private String title;
    private String description;
    private BigDecimal price;
    private UUID instructorId;
    private CourseStatus status;
    private List<String> categories;
    private List<String> tags;
    private BigDecimal discountPrice;
    private OffsetDateTime promoEndDate;
    private OffsetDateTime created;
    private OffsetDateTime updated;
    private UUID createdBy;
    private UUID updatedBy;
    private Boolean isActive;
}
