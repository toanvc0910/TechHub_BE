package com.techhub.app.courseservice.entity;

import com.techhub.app.commonservice.jpa.BooleanToYNStringConverter;
import com.techhub.app.commonservice.jpa.PostgreSQLEnumType;
import com.techhub.app.courseservice.enums.CourseLevel;
import com.techhub.app.courseservice.enums.CourseStatus;
import com.techhub.app.courseservice.enums.Language;
import com.vladmihalcea.hibernate.type.array.ListArrayType;
import com.vladmihalcea.hibernate.type.json.JsonType;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.Parameter;
import org.hibernate.annotations.Type;
import org.hibernate.annotations.TypeDef;
import org.hibernate.annotations.TypeDefs;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Convert;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import javax.persistence.PrePersist;
import javax.persistence.PreUpdate;
import javax.persistence.Table;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "courses")
@Getter
@Setter
@TypeDefs({
    @TypeDef(name = "pgsql_enum", typeClass = PostgreSQLEnumType.class),
    @TypeDef(name = "list-array", typeClass = ListArrayType.class),
    @TypeDef(name = "json", typeClass = JsonType.class)
})
public class Course {

    @Id
    @GeneratedValue(generator = "uuid2")
    @GenericGenerator(name = "uuid2", strategy = "uuid2")
    private UUID id;

    @Column(nullable = false, length = 255)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal price;

    @Column(name = "instructor_id", nullable = false)
    private UUID instructorId;

    @Enumerated(EnumType.STRING)
    @Type(type = "pgsql_enum", parameters = @Parameter(name = "enumClass", value = "com.techhub.app.courseservice.enums.CourseStatus"))
    @Column(name = "status", columnDefinition = "course_status")
    private CourseStatus status = CourseStatus.DRAFT;

    @Enumerated(EnumType.STRING)
    @Type(type = "pgsql_enum", parameters = @Parameter(name = "enumClass", value = "com.techhub.app.courseservice.enums.CourseLevel"))
    @Column(name = "level", columnDefinition = "course_level")
    private CourseLevel level = CourseLevel.ALL_LEVELS;

    @Enumerated(EnumType.STRING)
    @Type(type = "pgsql_enum", parameters = @Parameter(name = "enumClass", value = "com.techhub.app.courseservice.enums.Language"))
    @Column(name = "language", columnDefinition = "lang")
    private Language language = Language.VI;

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

    @Column(name = "thumbnail_file_id")
    private UUID thumbnailFileId;

    @Column(name = "intro_video_file_id")
    private UUID introVideoFileId;

    @Type(type = "json")
    @Column(name = "objectives", columnDefinition = "jsonb")
    private List<String> objectives;

    @Type(type = "json")
    @Column(name = "requirements", columnDefinition = "jsonb")
    private List<String> requirements;

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
    private Boolean isActive = Boolean.TRUE;

    @OneToMany(mappedBy = "course", cascade = CascadeType.ALL, orphanRemoval = false)
    private List<Chapter> chapters = new ArrayList<>();

    @PrePersist
    void beforeInsert() {
        OffsetDateTime now = OffsetDateTime.now();
        created = now;
        updated = now;
        if (categories == null) {
            categories = new ArrayList<>();
        }
        if (tags == null) {
            tags = new ArrayList<>();
        }
        if (objectives == null) {
            objectives = new ArrayList<>();
        }
        if (requirements == null) {
            requirements = new ArrayList<>();
        }
        if (isActive == null) {
            isActive = Boolean.TRUE;
        }
    }

    @PreUpdate
    void beforeUpdate() {
        updated = OffsetDateTime.now();
        if (categories == null) {
            categories = new ArrayList<>();
        }
        if (tags == null) {
            tags = new ArrayList<>();
        }
        if (objectives == null) {
            objectives = new ArrayList<>();
        }
        if (requirements == null) {
            requirements = new ArrayList<>();
        }
        if (isActive == null) {
            isActive = Boolean.TRUE;
        }
    }
}
