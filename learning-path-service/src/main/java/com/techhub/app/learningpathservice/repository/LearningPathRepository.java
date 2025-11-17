package com.techhub.app.learningpathservice.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.techhub.app.learningpathservice.entity.LearningPath;

@Repository
public interface LearningPathRepository extends JpaRepository<LearningPath, UUID> {
    List<LearningPath> findByIsActive(String isActive);

    @Query("SELECT lp FROM LearningPath lp WHERE lp.title LIKE %:title% AND lp.isActive ='Y' ")
    List<LearningPath> searchByTitle(@Param("title") String title);
}
