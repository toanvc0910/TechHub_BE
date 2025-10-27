package com.techhub.app.fileservice.entity;

import com.techhub.app.fileservice.enums.FileTypeEnum;
import com.vladmihalcea.hibernate.type.array.StringArrayType;
import com.vladmihalcea.hibernate.type.basic.PostgreSQLEnumType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.Type;
import org.hibernate.annotations.TypeDef;
import org.hibernate.annotations.TypeDefs;

import javax.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "files")
@Data
@NoArgsConstructor
@AllArgsConstructor
@TypeDefs({
        @TypeDef(name = "string-array", typeClass = StringArrayType.class),
        @TypeDef(name = "pgsql_enum", typeClass = PostgreSQLEnumType.class)
})
public class FileEntity {

    @Id
    @GeneratedValue(generator = "UUID")
    @GenericGenerator(name = "UUID", strategy = "org.hibernate.id.UUIDGenerator")
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "folder_id")
    private UUID folderId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "folder_id", insertable = false, updatable = false)
    private FileFolderEntity folder;

    @Column(name = "name", nullable = false, length = 500)
    private String name;

    @Column(name = "original_name", nullable = false, length = 500)
    private String originalName;

    @Enumerated(EnumType.STRING)
    @Type(type = "pgsql_enum")
    @Column(name = "file_type", nullable = false, columnDefinition = "file_type_enum")
    private FileTypeEnum fileType;

    @Column(name = "mime_type", length = 100)
    private String mimeType;

    @Column(name = "file_size")
    private Long fileSize;

    @Column(name = "cloudinary_public_id", nullable = false, length = 500)
    private String cloudinaryPublicId;

    @Column(name = "cloudinary_url", nullable = false, columnDefinition = "TEXT")
    private String cloudinaryUrl;

    @Column(name = "cloudinary_secure_url", columnDefinition = "TEXT")
    private String cloudinarySecureUrl;

    @Column(name = "width")
    private Integer width;

    @Column(name = "height")
    private Integer height;

    @Column(name = "duration")
    private Integer duration;

    @Column(name = "alt_text", length = 500)
    private String altText;

    @Column(name = "caption", columnDefinition = "TEXT")
    private String caption;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Type(type = "string-array")
    @Column(name = "tags", columnDefinition = "text[]")
    private String[] tags;

    @Column(name = "upload_source", length = 50)
    private String uploadSource = "DIRECT";

    @Column(name = "reference_id")
    private UUID referenceId;

    @Column(name = "reference_type", length = 50)
    private String referenceType;

    @Column(name = "created", nullable = false)
    private LocalDateTime created;

    @Column(name = "updated", nullable = false)
    private LocalDateTime updated;

    @Column(name = "created_by")
    private UUID createdBy;

    @Column(name = "updated_by")
    private UUID updatedBy;

    @Column(name = "is_active", nullable = false, length = 1)
    private String isActive = "Y";

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        created = now;
        updated = now;
        if (uploadSource == null) {
            uploadSource = "DIRECT";
        }
        if (isActive == null) {
            isActive = "Y";
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updated = LocalDateTime.now();
    }
}
