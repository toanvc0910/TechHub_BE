package com.techhub.app.courseservice.repository;

import com.techhub.app.courseservice.entity.Skill;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;

public interface SkillRepository extends JpaRepository<Skill, UUID> {
}
