package com.techhub.app.courseservice.entity;

import com.techhub.app.commonservice.jpa.BooleanToYNStringConverter;
import com.vladmihalcea.hibernate.type.json.JsonType;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.TypeDef;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Convert;
import javax.persistence.Entity;
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
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "chapters")
@Getter
@Setter
@TypeDef(name = "json", typeClass = JsonType.class)
public class Chapter {

    @Id
    @GeneratedValue(generator = "uuid2")
    @GenericGenerator(name = "uuid2", strategy = "uuid2")
    private UUID id;

    @Column(nullable = false, length = 255)
    private String title;

    @Column(name = "\"order\"", nullable = false)
    private Integer orderIndex;

    @ManyToOne(optional = false)
    @JoinColumn(name = "course_id", nullable = false)
    private Course course;

    @Column(name = "min_completion_threshold")
    private Float minCompletionThreshold = 0.7f;

    @Column(name = "auto_unlock")
    private Boolean autoUnlock = Boolean.TRUE;

    @Column(name = "locked")
    private Boolean locked = Boolean.TRUE;

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

    @OneToMany(mappedBy = "chapter", cascade = CascadeType.ALL, orphanRemoval = false)
    private List<Lesson> lessons = new ArrayList<>();

    @PrePersist
    void beforeInsert() {
        OffsetDateTime now = OffsetDateTime.now();
        created = now;
        updated = now;
        if (isActive == null) {
            isActive = Boolean.TRUE;
        }
    }

    @PreUpdate
    void beforeUpdate() {
        updated = OffsetDateTime.now();
        if (isActive == null) {
            isActive = Boolean.TRUE;
        }
    }
}
