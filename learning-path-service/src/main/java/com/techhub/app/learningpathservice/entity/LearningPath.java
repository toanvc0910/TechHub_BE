package com.techhub.app.learningpathservice.entity;

import com.techhub.app.commonservice.jpa.BooleanToYNStringConverter;
import com.techhub.app.commonservice.jpa.PostgreSQLEnumType;
import com.vladmihalcea.hibernate.type.json.JsonType;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.Type;
import org.hibernate.annotations.TypeDef;
import org.hibernate.annotations.TypeDefs;

import javax.persistence.*;
import java.io.Serializable;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "learning_paths")
@Getter
@Setter
@TypeDefs({
        @TypeDef(name = "pgsql_enum", typeClass = PostgreSQLEnumType.class),
        @TypeDef(name = "json", typeClass = JsonType.class)
})
public class LearningPath {

    @Id
    @GeneratedValue(generator = "uuid2")
    @GenericGenerator(name = "uuid2", strategy = "uuid2")
    private UUID id;

    @Column(nullable = false, length = 255)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @OneToMany(mappedBy = "learningPath", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<LearningPathSkill> pathSkills = new ArrayList<>();

    @Type(type = "json")
    @Column(name = "layout_edges", columnDefinition = "jsonb")
    private List<LayoutEdge> layoutEdges;

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

    @OneToMany(mappedBy = "learningPath", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("order ASC")
    private List<LearningPathCourse> courses = new ArrayList<>();

    @PrePersist
    void beforeInsert() {
        OffsetDateTime now = OffsetDateTime.now();
        created = now;
        updated = now;
        if (pathSkills == null) {
            pathSkills = new ArrayList<>();
        }
        if (layoutEdges == null) {
            layoutEdges = new ArrayList<>();
        }
        if (isActive == null) {
            isActive = Boolean.TRUE;
        }
    }

    @PreUpdate
    void beforeUpdate() {
        updated = OffsetDateTime.now();
        if (pathSkills == null) {
            pathSkills = new ArrayList<>();
        }
        if (layoutEdges == null) {
            layoutEdges = new ArrayList<>();
        }
        if (isActive == null) {
            isActive = Boolean.TRUE;
        }
    }

    /**
     * Inner class to represent a visual designer edge connection
     */
    @Getter
    @Setter
    public static class LayoutEdge implements Serializable {
        private String source;
        private String target;

        public LayoutEdge() {
        }

        public LayoutEdge(String source, String target) {
            this.source = source;
            this.target = target;
        }
    }
}
