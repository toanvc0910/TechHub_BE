package com.techhub.app.userservice.entity;

import com.techhub.app.commonservice.jpa.BooleanToYNStringConverter;
import com.techhub.app.userservice.enums.InstructorApplicationAdminStatus;
import com.techhub.app.userservice.enums.InstructorApplicationAiStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.Type;
import org.hibernate.annotations.TypeDef;

import javax.persistence.Column;
import javax.persistence.Convert;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.PrePersist;
import javax.persistence.PreUpdate;
import javax.persistence.Table;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "instructor_applications")
@TypeDef(name = "jsonb", typeClass = com.techhub.app.userservice.config.JsonbType.class)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InstructorApplication {

    @Id
    @GeneratedValue(generator = "uuid2")
    @GenericGenerator(name = "uuid2", strategy = "uuid2")
    @Column(name = "id")
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "cv_file_id", nullable = false)
    private UUID cvFileId;

    @Column(name = "cv_file_url", columnDefinition = "TEXT")
    private String cvFileUrl;

    @Enumerated(EnumType.STRING)
    @Column(name = "ai_status", nullable = false, length = 20)
    private InstructorApplicationAiStatus aiStatus = InstructorApplicationAiStatus.PENDING;

    @Type(type = "jsonb")
    @Column(name = "ai_extracted_data", columnDefinition = "jsonb")
    private String aiExtractedData;

    @Column(name = "ai_error", columnDefinition = "TEXT")
    private String aiError;

    @Enumerated(EnumType.STRING)
    @Column(name = "admin_status", nullable = false, length = 20)
    private InstructorApplicationAdminStatus adminStatus = InstructorApplicationAdminStatus.PENDING;

    @Column(name = "admin_note", columnDefinition = "TEXT")
    private String adminNote;

    @Column(name = "reviewed_by")
    private UUID reviewedBy;

    @Column(name = "reviewed_at")
    private LocalDateTime reviewedAt;

    @Column(name = "created", nullable = false)
    private LocalDateTime created;

    @Column(name = "updated", nullable = false)
    private LocalDateTime updated;

    @Convert(converter = BooleanToYNStringConverter.class)
    @Column(name = "is_active", nullable = false, length = 1)
    private Boolean isActive = true;

    @PrePersist
    protected void onCreate() {
        created = LocalDateTime.now();
        updated = LocalDateTime.now();
        if (aiStatus == null) aiStatus = InstructorApplicationAiStatus.PENDING;
        if (adminStatus == null) adminStatus = InstructorApplicationAdminStatus.PENDING;
        if (isActive == null) isActive = true;
    }

    @PreUpdate
    protected void onUpdate() {
        updated = LocalDateTime.now();
    }
}
