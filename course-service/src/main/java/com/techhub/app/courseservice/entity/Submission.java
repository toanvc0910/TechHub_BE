package com.techhub.app.courseservice.entity;

import com.techhub.app.commonservice.jpa.BooleanToYNStringConverter;
import com.techhub.app.commonservice.jpa.PostgreSQLEnumType;
import com.techhub.app.courseservice.enums.SubmissionStatus;
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
import java.util.UUID;

@Entity
@Table(name = "submissions")
@Getter
@Setter
@TypeDefs({
    @TypeDef(name = "pgsql_enum", typeClass = PostgreSQLEnumType.class),
    @TypeDef(name = "json", typeClass = JsonType.class)
})
public class Submission {

    @Id
    @GeneratedValue(generator = "uuid2")
    @GenericGenerator(name = "uuid2", strategy = "uuid2")
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @ManyToOne(optional = false)
    @JoinColumn(name = "exercise_id", nullable = false)
    private Exercise exercise;

    @Column(name = "answer", columnDefinition = "TEXT")
    private String answer;

    @Type(type = "json")
    @Column(name = "submission_data", columnDefinition = "jsonb")
    private Object submissionData;

    @Column(name = "grade")
    private Float grade;

    @Column(name = "graded_at")
    private OffsetDateTime gradedAt;

    @Column(name = "graded_by")
    private UUID gradedBy;

    @Column(name = "created", nullable = false)
    private OffsetDateTime created;

    @Column(name = "updated", nullable = false)
    private OffsetDateTime updated;

    @Enumerated(EnumType.STRING)
    @Type(type = "pgsql_enum", parameters = @Parameter(name = "enumClass", value = "com.techhub.app.courseservice.enums.SubmissionStatus"))
    @Column(name = "status", columnDefinition = "submission_status")
    private SubmissionStatus status = SubmissionStatus.PENDING;

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
        if (isActive == null) {
            isActive = Boolean.TRUE;
        }
        if (status == null) {
            status = SubmissionStatus.PENDING;
        }
    }

    @PreUpdate
    void beforeUpdate() {
        updated = OffsetDateTime.now();
        if (isActive == null) {
            isActive = Boolean.TRUE;
        }
        if (status == null) {
            status = SubmissionStatus.PENDING;
        }
    }
}
