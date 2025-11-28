package com.techhub.app.learningpathservice.repository;

import com.techhub.app.learningpathservice.entity.LearningPathSkill;
import com.techhub.app.learningpathservice.entity.LearningPathSkillId;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LearningPathSkillRepository extends JpaRepository<LearningPathSkill, LearningPathSkillId> {
}
