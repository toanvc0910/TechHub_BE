package com.techhub.app.courseservice.dto;

import com.techhub.app.courseservice.enums.CourseStatus;
import lombok.Data;

import javax.validation.constraints.*;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Data
public class CourseRequest {
    @NotBlank
    @Size(max = 255)
    private String title;

    private String description;

    @NotNull
    @DecimalMin(value = "0.0", inclusive = true)
    private BigDecimal price;

    @NotNull
    private UUID instructorId;

    private CourseStatus status;

    private List<@Size(max = 128) String> categories;
    private List<@Size(max = 128) String> tags;

    @DecimalMin(value = "0.0", inclusive = true)
    private BigDecimal discountPrice;

    private OffsetDateTime promoEndDate;

    private Boolean isActive;
}
