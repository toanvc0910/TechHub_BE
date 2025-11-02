package com.techhub.app.courseservice.dto.request;

import com.techhub.app.courseservice.enums.CourseLevel;
import com.techhub.app.courseservice.enums.CourseStatus;
import com.techhub.app.courseservice.enums.Language;
import lombok.Getter;
import lombok.Setter;

import javax.validation.constraints.DecimalMin;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
public class CourseRequest {

    @NotBlank
    @Size(max = 255)
    private String title;

    @Size(max = 5000)
    private String description;

    @NotNull
    @DecimalMin(value = "0.0", inclusive = true)
    private BigDecimal price;

    private CourseStatus status;

    private CourseLevel level;

    private Language language;

    private List<@Size(max = 100) String> categories;

    private List<@Size(max = 100) String> tags;

    @DecimalMin(value = "0.0", inclusive = true)
    private BigDecimal discountPrice;

    private OffsetDateTime promoEndDate;

    private UUID thumbnailFileId;

    private UUID introVideoFileId;

    private List<@Size(max = 255) String> objectives;

    private List<@Size(max = 255) String> requirements;

    /**
     * Optional instructor id (allowed for admins when creating/updating courses on behalf of others)
     */
    private UUID instructorId;
}
