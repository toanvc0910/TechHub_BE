package com.techhub.app.fileservice.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.GenericGenerator;

import javax.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "file_usage")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class FileUsageEntity {

    @Id
    @GeneratedValue(generator = "UUID")
    @GenericGenerator(name = "UUID", strategy = "org.hibernate.id.UUIDGenerator")
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "file_id", nullable = false)
    private UUID fileId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "file_id", insertable = false, updatable = false)
    private FileEntity file;

    @Column(name = "used_in_type", nullable = false, length = 50)
    private String usedInType;

    @Column(name = "used_in_id", nullable = false)
    private UUID usedInId;

    @Column(name = "created", nullable = false)
    private LocalDateTime created;

    @PrePersist
    protected void onCreate() {
        created = LocalDateTime.now();
    }
}
