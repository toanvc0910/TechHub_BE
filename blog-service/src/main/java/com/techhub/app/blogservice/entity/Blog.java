package com.techhub.app.blogservice.entity;

import com.techhub.app.blogservice.enums.BlogStatus;
import com.techhub.app.blogservice.model.BlogAttachment;
import com.techhub.app.commonservice.jpa.BooleanToYNStringConverter;
import com.techhub.app.commonservice.jpa.PostgreSQLEnumType;
import com.vladmihalcea.hibernate.type.array.ListArrayType;
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
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.PrePersist;
import javax.persistence.PreUpdate;
import javax.persistence.Table;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "blogs")
@Getter
@Setter
@TypeDefs({
        @TypeDef(name = "pgsql_enum", typeClass = PostgreSQLEnumType.class),
        @TypeDef(name = "list-array", typeClass = ListArrayType.class),
        @TypeDef(name = "json", typeClass = JsonType.class)
})
public class Blog {

    @Id
    @GeneratedValue(generator = "uuid2")
    @GenericGenerator(name = "uuid2", strategy = "uuid2")
    private UUID id;

    @Column(nullable = false, length = 255)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(name = "author_id", nullable = false)
    private UUID authorId;

    @Type(type = "list-array")
    @Column(name = "tags", columnDefinition = "text[]")
    private List<String> tags;

    @Type(type = "json")
    @Column(name = "attachments", columnDefinition = "jsonb")
    private List<BlogAttachment> attachments;

    @Type(type = "pgsql_enum", parameters = @Parameter(name = "enumClass", value = "com.techhub.app.blogservice.enums.BlogStatus"))
    @Column(name = "status", columnDefinition = "blog_status")
    private BlogStatus status = BlogStatus.DRAFT;

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
    void onCreate() {
        OffsetDateTime now = OffsetDateTime.now();
        created = now;
        updated = now;
        if (isActive == null) {
            isActive = Boolean.TRUE;
        }
    }

    @PreUpdate
    void onUpdate() {
        updated = OffsetDateTime.now();
        if (isActive == null) {
            isActive = Boolean.TRUE;
        }
    }
}
