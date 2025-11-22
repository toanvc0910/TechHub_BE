package com.techhub.app.learningpathservice.repository;

import com.techhub.app.learningpathservice.entity.LearningPathSkill;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;

public interface LearningPathSkillRepository extends JpaRepository<LearningPathSkill, UUID> {
}
