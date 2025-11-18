package com.techhub.app.learningpathservice.entity;

import lombok.Getter;
import lombok.Setter;

import javax.persistence.*;
import java.io.Serializable;
import java.util.UUID;

@Entity
@Table(name = "learning_path_courses")
@Getter
@Setter
@IdClass(LearningPathCourse.LearningPathCourseId.class)
public class LearningPathCourse {

    @Id
    @Column(name = "path_id")
    private UUID pathId;

    @Id
    @Column(name = "course_id")
    private UUID courseId;

    @Column(name = "\"order\"", nullable = false)
    private Integer order;

    @Column(name = "position_x")
    private Integer positionX;

    @Column(name = "position_y")
    private Integer positionY;

    @Column(name = "is_optional", length = 1)
    private String isOptional = "N";

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "path_id", insertable = false, updatable = false)
    private LearningPath learningPath;

    @Getter
    @Setter
    public static class LearningPathCourseId implements Serializable {
        private UUID pathId;
        private UUID courseId;

        public LearningPathCourseId() {
        }

        public LearningPathCourseId(UUID pathId, UUID courseId) {
            this.pathId = pathId;
            this.courseId = courseId;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o)
                return true;
            if (o == null || getClass() != o.getClass())
                return false;
            LearningPathCourseId that = (LearningPathCourseId) o;
            return pathId.equals(that.pathId) && courseId.equals(that.courseId);
        }

        @Override
        public int hashCode() {
            return 31 * pathId.hashCode() + courseId.hashCode();
        }
    }
}
