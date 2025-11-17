package com.techhub.app.learningpathservice.entity;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.Type;

@Entity
@Table
public class PathProgress {
    @Id
    @GeneratedValue(generator = "uuid2")
    @GenericGenerator(name = "uuid2", strategy = "uuid2")
    @Column(name = "id")
    private UUID id;

    @Column(nullable = false)
    private UUID userId;

    @Column(nullable = false)
    private UUID pathId;

    @Column(nullable = false)
    private Float completion = 0.0f;

    @Type(type = "json")
    @Column(name = "milestones", columnDefinition = "jsonb")
    private List<String> milestones = new ArrayList<>();
    @Column(name = "created", nullable = false)
    private LocalDateTime created;

    @Column(name = "updated", nullable = false)
    private LocalDateTime updated;

    @Column(name = "created_by")
    private UUID createdBy;

    @Column(name = "updated_by")
    private UUID updatedBy;

    @Column(nullable = false, length = 1)
    private String isActive = "Y";

    @ManyToOne
    @JoinColumn(name = "path_id", insertable = false, updatable = false)
    private LearningPath learningPath;
}
