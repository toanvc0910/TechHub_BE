package com.techhub.app.courseservice.entity;

import com.techhub.app.commonservice.jpa.BooleanToYNStringConverter;
import com.techhub.app.courseservice.enums.LessonAssetType;
import com.vladmihalcea.hibernate.type.json.JsonType;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.Parameter;
import org.hibernate.annotations.Type;
import org.hibernate.annotations.TypeDef;
import org.hibernate.annotations.TypeDefs;

import javax.persistence.Column;
import javax.persistence.Convert;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.PrePersist;
import javax.persistence.PreUpdate;
import javax.persistence.Table;
import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "lesson_assets")
@Getter
@Setter
@TypeDefs({
    @TypeDef(name = "pgsql_enum", typeClass = com.techhub.app.commonservice.jpa.PostgreSQLEnumType.class),
    @TypeDef(name = "json", typeClass = JsonType.class)
})
public class LessonAsset {

    @Id
    @GeneratedValue(generator = "uuid2")
    @GenericGenerator(name = "uuid2", strategy = "uuid2")
    private UUID id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "lesson_id", nullable = false)
    private Lesson lesson;

    @Enumerated(EnumType.STRING)
    @Type(type = "pgsql_enum", parameters = @Parameter(name = "enumClass", value = "com.techhub.app.courseservice.enums.LessonAssetType"))
    @Column(name = "asset_type", columnDefinition = "lesson_asset_type", nullable = false)
    private LessonAssetType assetType;

    @Column(name = "\"order\"", nullable = false)
    private Integer orderIndex = 0;

    @Column(name = "title", length = 255)
    private String title;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "file_id")
    private UUID fileId;

    @Column(name = "external_url")
    private String externalUrl;

    @Type(type = "json")
    @Column(name = "metadata", columnDefinition = "jsonb")
    private Map<String, Object> metadata;

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

    @PrePersist
    void beforeInsert() {
        OffsetDateTime now = OffsetDateTime.now();
        created = now;
        updated = now;
        if (metadata == null) {
            metadata = new HashMap<>();
        }
        if (isActive == null) {
            isActive = Boolean.TRUE;
        }
    }

    @PreUpdate
    void beforeUpdate() {
        updated = OffsetDateTime.now();
        if (metadata == null) {
            metadata = new HashMap<>();
        }
        if (isActive == null) {
            isActive = Boolean.TRUE;
        }
    }
}
