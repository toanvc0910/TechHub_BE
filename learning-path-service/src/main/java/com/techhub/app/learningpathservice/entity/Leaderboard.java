package com.techhub.app.learningpathservice.entity;

import java.security.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;

import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.Type;

import com.techhub.app.learningpathservice.enums.LeaderboardType;

@Entity
@Table(name = "leaderboards")
public class Leaderboard {
    @Id
    @GeneratedValue(generator = "uuid2")
    @GenericGenerator(name = "uuid2", strategy = "uuid2")
    @Column(name = "id")
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private LeaderboardType type; // GLOBAL, COURSE, PATH

    @Column(nullable = false)
    private UUID courseId;

    @Column(nullable = false)
    private UUID pathId;

    @Type(type = "json")
    @Column(name = "scores", columnDefinition = "jsonb")
    private List<String> scores = new ArrayList<>();

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

}