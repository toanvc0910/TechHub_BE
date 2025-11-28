package com.techhub.app.learningpathservice.entity;

import lombok.Getter;
import lombok.Setter;

import javax.persistence.*;
import java.time.OffsetDateTime;

@Entity
@Table(name = "learning_path_skills")
@Getter
@Setter
@IdClass(LearningPathSkillId.class)
public class LearningPathSkill {

    @Id
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "path_id", nullable = false)
    private LearningPath learningPath;

    @Id
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "skill_id", nullable = false)
    private Skill skill;

    @Column(name = "assigned_at")
    private OffsetDateTime assignedAt;
}
