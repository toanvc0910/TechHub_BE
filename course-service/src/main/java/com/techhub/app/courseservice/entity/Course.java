package com.techhub.app.courseservice.entity;

import com.techhub.app.courseservice.enums.CourseStatus;
import com.techhub.app.courseservice.config.BooleanToYNStringConverter;
import com.techhub.app.courseservice.config.PostgreSQLEnumType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.Parameter;
import org.hibernate.annotations.Type;
import org.hibernate.annotations.TypeDef;

import javax.persistence.*;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "courses")
@TypeDef(name = "pgsql_enum", typeClass = PostgreSQLEnumType.class)
@Data
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class Course {

    @Id
    @GeneratedValue(generator = "uuid2")
    @GenericGenerator(name = "uuid2", strategy = "uuid2")
    @Column(name = "id")
    private UUID id;

    @Column(name = "title", nullable = false, length = 255)
    private String title;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "price", precision = 10, scale = 2, nullable = false)
    private BigDecimal price;

    @Column(name = "instructor_id", nullable = false)
    private UUID instructorId;

    @Type(type = "pgsql_enum", parameters = @Parameter(name = "enumClass", value = "com.techhub.app.courseservice.enums.CourseStatus"))
    @Column(name = "status", columnDefinition = "course_status")
    private CourseStatus status = CourseStatus.DRAFT;

    // Requires hibernate-types-52 dependency for list-array mapping
    @Type(type = "list-array")
    @Column(name = "categories", columnDefinition = "text[]")
    private List<String> categories;

    @Type(type = "list-array")
    @Column(name = "tags", columnDefinition = "text[]")
    private List<String> tags;

    @Column(name = "discount_price", precision = 10, scale = 2)
    private BigDecimal discountPrice;

    @Column(name = "promo_end_date")
    private OffsetDateTime promoEndDate;

    @Column(name = "created", nullable = false)
    private OffsetDateTime created;

    @Column(name = "updated", nullable = false)
    private OffsetDateTime updated;

    @Column(name = "created_by")
    private UUID createdBy;

    @Column(name = "updated_by")
    private UUID updatedBy;

    @Convert(converter = BooleanToYNStringConverter.class)
    @Column(name = "is_active", nullable = false, length = 1)
    private Boolean isActive = true;

    @PrePersist
    protected void onCreate() {
        created = OffsetDateTime.now();
        updated = OffsetDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updated = OffsetDateTime.now();
    }
}
