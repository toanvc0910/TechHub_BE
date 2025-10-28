package com.techhub.app.fileservice.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.GenericGenerator;

import javax.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "file_folders")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class FileFolderEntity {

    @Id
    @GeneratedValue(generator = "UUID")
    @GenericGenerator(name = "UUID", strategy = "org.hibernate.id.UUIDGenerator")
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "parent_id")
    private UUID parentId;

    @Column(name = "name", nullable = false, length = 255)
    private String name;

    @Column(name = "path", nullable = false, columnDefinition = "TEXT")
    private String path;

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
        if (isActive == null) {
            isActive = "Y";
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updated = LocalDateTime.now();
    }
}
