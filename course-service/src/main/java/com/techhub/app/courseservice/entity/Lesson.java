package com.techhub.app.courseservice.entity;

import com.techhub.app.commonservice.jpa.BooleanToYNStringConverter;
import com.techhub.app.commonservice.jpa.PostgreSQLEnumType;
import com.techhub.app.courseservice.enums.ContentType;
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
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.PrePersist;
import javax.persistence.PreUpdate;
import javax.persistence.Table;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "lessons")
@Getter
@Setter
@TypeDefs({
        @TypeDef(name = "pgsql_enum", typeClass = PostgreSQLEnumType.class),
        @TypeDef(name = "list-array", typeClass = ListArrayType.class),
        @TypeDef(name = "json", typeClass = JsonType.class)
})
public class Lesson {

    @Id
    @GeneratedValue(generator = "uuid2")
    @GenericGenerator(name = "uuid2", strategy = "uuid2")
    private UUID id;

    @Column(nullable = false, length = 255)
    private String title;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "\"order\"", nullable = false)
    private Integer orderIndex;

    @ManyToOne(optional = false)
    @JoinColumn(name = "chapter_id", nullable = false)
    private Chapter chapter;

    @Enumerated(EnumType.STRING)
    @Type(type = "pgsql_enum", parameters = @Parameter(name = "enumClass", value = "com.techhub.app.courseservice.enums.ContentType"))
    @Column(name = "content_type", columnDefinition = "content_type", nullable = false)
    private ContentType contentType;

    @Column(name = "content", columnDefinition = "TEXT")
    private String content;

    @Column(name = "mandatory")
    private Boolean mandatory = Boolean.TRUE;

    @Column(name = "completion_weight")
    private Float completionWeight = 1.0f;

    @Column(name = "estimated_duration")
    private Integer estimatedDuration;

    @Column(name = "is_free")
    private Boolean isFree = Boolean.FALSE;

    @Column(name = "workspace_enabled")
    private Boolean workspaceEnabled = Boolean.FALSE;

    @Type(type = "list-array")
    @Column(name = "workspace_languages", columnDefinition = "text[]")
    private List<String> workspaceLanguages;

    @Type(type = "json")
    @Column(name = "workspace_template", columnDefinition = "jsonb")
    private Map<String, Object> workspaceTemplate;

    @Column(name = "video_url")
    private String videoUrl;

    @Type(type = "json")
    @Column(name = "document_urls", columnDefinition = "jsonb")
    private List<String> documentUrls;

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

    @OneToMany(mappedBy = "lesson", cascade = CascadeType.ALL, orphanRemoval = false)
    private List<LessonAsset> assets = new ArrayList<>();

    @PrePersist
    void beforeInsert() {
        OffsetDateTime now = OffsetDateTime.now();
        created = now;
        updated = now;
        if (workspaceLanguages == null) {
            workspaceLanguages = new ArrayList<>();
        }
        if (workspaceTemplate == null) {
            workspaceTemplate = new HashMap<>();
        }
        if (documentUrls == null) {
            documentUrls = new ArrayList<>();
        }
        if (isActive == null) {
            isActive = Boolean.TRUE;
        }
    }

    @PreUpdate
    void beforeUpdate() {
        updated = OffsetDateTime.now();
        if (workspaceLanguages == null) {
            workspaceLanguages = new ArrayList<>();
        }
        if (workspaceTemplate == null) {
            workspaceTemplate = new HashMap<>();
        }
        if (documentUrls == null) {
            documentUrls = new ArrayList<>();
        }
        if (isActive == null) {
            isActive = Boolean.TRUE;
        }
    }
}
