package com.techhub.app.learningpathservice.entity;

import java.util.UUID;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "learning_path_courses")
public class LearningPathCourse {
    @Id
    private UUID pathId;

    @Id
    private UUID courseId;

    @Column(nullable = false, name = "\"order\"")
    private Integer order;

    // Relationships
    @ManyToOne
    @JoinColumn(name = "path_id", insertable = false, updatable = false)
    private LearningPath learningPath;

}
