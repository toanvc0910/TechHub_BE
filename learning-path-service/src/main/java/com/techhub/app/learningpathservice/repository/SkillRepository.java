package com.techhub.app.learningpathservice.repository;

import com.techhub.app.learningpathservice.entity.Skill;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.UUID;

public interface SkillRepository extends JpaRepository<Skill, UUID> {
    Optional<Skill> findByName(String name);
}
