package com.techhub.app.learningpathservice.entity;

import com.techhub.app.learningpathservice.enums.SkillCategory;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.*;
import java.util.UUID;

@Entity
@Table(name = "skills")
@Data
@AllArgsConstructor
@NoArgsConstructor
public class Skill {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "name", nullable = false, unique = true)
    private String name;

    @Column(name = "thumbnail")
    private String thumbnail;

    @Column(name = "category", columnDefinition = "skill_category")
    @Enumerated(EnumType.STRING)
    @org.hibernate.annotations.Type(type = "com.techhub.app.learningpathservice.util.PostgreSQLEnumType")
    private SkillCategory category;
}
