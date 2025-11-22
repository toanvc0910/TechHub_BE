package com.techhub.app.learningpathservice.service.impl;

import com.techhub.app.learningpathservice.dto.*;
import com.techhub.app.learningpathservice.entity.LearningPath;
import com.techhub.app.learningpathservice.entity.LearningPathCourse;
import com.techhub.app.learningpathservice.entity.LearningPathSkill;
import com.techhub.app.learningpathservice.entity.Skill;
import com.techhub.app.learningpathservice.mapper.LearningPathMapper;
import com.techhub.app.learningpathservice.repository.LearningPathCourseRepository;
import com.techhub.app.learningpathservice.repository.LearningPathRepository;
import com.techhub.app.learningpathservice.repository.LearningPathSkillRepository;
import com.techhub.app.learningpathservice.repository.SkillRepository;
import com.techhub.app.learningpathservice.service.LearningPathService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class LearningPathServiceImpl implements LearningPathService {

    private final LearningPathRepository learningPathRepository;
    private final LearningPathCourseRepository learningPathCourseRepository;
    private final LearningPathMapper learningPathMapper;
    private final SkillRepository skillRepository;
    private final LearningPathSkillRepository learningPathSkillRepository;

    @Override
    public LearningPathResponseDTO createLearningPath(LearningPathRequestDTO requestDTO) {
        log.info("Creating new learning path with title: {}", requestDTO.getTitle());

        LearningPath learningPath = learningPathMapper.toEntity(requestDTO);

        // Map skills to learning path
        if (requestDTO.getSkills() != null) {
            mapSkillsToPath(learningPath, requestDTO.getSkills());
        }

        learningPath = learningPathRepository.save(learningPath);

        log.info("Learning path created successfully with ID: {}", learningPath.getId());
        return learningPathMapper.toDTO(learningPath);
    }

    @Override
    public LearningPathResponseDTO updateLearningPath(UUID id, LearningPathRequestDTO requestDTO) {
        log.info("Updating learning path with ID: {}", id);

        LearningPath learningPath = learningPathRepository.findByIdAndIsActive(id, Boolean.TRUE)
                .orElseThrow(() -> new RuntimeException("Learning path not found with ID: " + id));

        learningPathMapper.updateEntity(learningPath, requestDTO);

        // Map skills to learning path
        if (requestDTO.getSkills() != null) {
            mapSkillsToPath(learningPath, requestDTO.getSkills());
        }

        learningPath = learningPathRepository.save(learningPath);

        log.info("Learning path updated successfully with ID: {}", id);
        return learningPathMapper.toDTO(learningPath);
    }

    @Override
    @Transactional(readOnly = true)
    public LearningPathResponseDTO getLearningPathById(UUID id) {
        log.info("=".repeat(80));
        log.info("🔍 GET LEARNING PATH BY ID - START");
        log.info("Fetching learning path with ID: {}", id);

        LearningPath learningPath = learningPathRepository.findByIdAndIsActive(id, Boolean.TRUE)
                .orElseThrow(() -> new RuntimeException("Learning path not found with ID: " + id));

        log.info("📊 Learning path found: title={}, courses count={}",
                learningPath.getTitle(),
                learningPath.getCourses() != null ? learningPath.getCourses().size() : 0);

        if (learningPath.getCourses() != null) {
            log.info("\n📋 Courses from database:");
            for (LearningPathCourse course : learningPath.getCourses()) {
                log.info("   📌 Course: courseId={}, order={}, positionX={}, positionY={}, isOptional={}",
                        course.getCourseId(), course.getOrder(),
                        course.getPositionX(), course.getPositionY(),
                        course.getIsOptional());
            }
        }

        LearningPathResponseDTO response = learningPathMapper.toDTO(learningPath);

        log.info("\n📤 Response DTO:");
        if (response.getCourses() != null) {
            for (CourseInPathDTO courseDTO : response.getCourses()) {
                log.info("   📦 Course DTO: courseId={}, order={}, positionX={}, positionY={}, isOptional={}",
                        courseDTO.getCourseId(), courseDTO.getOrder(),
                        courseDTO.getPositionX(), courseDTO.getPositionY(),
                        courseDTO.getIsOptional());
            }
        }

        log.info("🔍 GET LEARNING PATH BY ID - END");
        log.info("=".repeat(80));

        return response;
    }

    @Override
    @Transactional(readOnly = true)
    public Page<LearningPathResponseDTO> getAllLearningPaths(Pageable pageable) {
        log.info("Fetching all learning paths");

        Page<LearningPath> learningPaths = learningPathRepository.findByIsActive(Boolean.TRUE, pageable);
        return learningPaths.map(learningPathMapper::toDTO);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<LearningPathResponseDTO> searchLearningPaths(String keyword, Pageable pageable) {
        log.info("Searching learning paths with keyword: {}", keyword);

        Page<LearningPath> learningPaths = learningPathRepository.searchLearningPaths(keyword, pageable);
        return learningPaths.map(learningPathMapper::toDTO);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<LearningPathResponseDTO> getLearningPathsByCreator(UUID userId, Pageable pageable) {
        log.info("Fetching learning paths created by user ID: {}", userId);

        Page<LearningPath> learningPaths = learningPathRepository.findByCreatedBy(userId, pageable);
        return learningPaths.map(learningPathMapper::toDTO);
    }

    @Override
    public void deleteLearningPath(UUID id) {
        log.info("Deleting learning path with ID: {}", id);

        LearningPath learningPath = learningPathRepository.findByIdAndIsActive(id, Boolean.TRUE)
                .orElseThrow(() -> new RuntimeException("Learning path not found with ID: " + id));

        learningPath.setIsActive(Boolean.FALSE);
        learningPathRepository.save(learningPath);

        log.info("Learning path deleted successfully with ID: {}", id);
    }

    @Override
    public LearningPathResponseDTO addCoursesToPath(UUID pathId, AddCoursesToPathRequestDTO requestDTO) {
        log.info("Adding courses to learning path ID: {}", pathId);

        LearningPath learningPath = learningPathRepository.findByIdAndIsActive(pathId, Boolean.TRUE)
                .orElseThrow(() -> new RuntimeException("Learning path not found with ID: " + pathId));

        for (CourseInPathDTO courseDTO : requestDTO.getCourses()) {
            // Check if course already exists in path
            if (learningPathCourseRepository.existsByPathIdAndCourseId(pathId, courseDTO.getCourseId())) {
                log.warn("Course {} already exists in path {}", courseDTO.getCourseId(), pathId);
                continue;
            }

            LearningPathCourse pathCourse = new LearningPathCourse();
            pathCourse.setPathId(pathId);
            pathCourse.setCourseId(courseDTO.getCourseId());
            pathCourse.setOrder(courseDTO.getOrder());

            learningPathCourseRepository.save(pathCourse);
        }

        // Refresh to get updated courses
        learningPath = learningPathRepository.findById(pathId).orElseThrow();

        log.info("Courses added successfully to learning path ID: {}", pathId);
        return learningPathMapper.toDTO(learningPath);
    }

    @Override
    public LearningPathResponseDTO removeCourseFromPath(UUID pathId, UUID courseId) {
        log.info("Removing course {} from learning path {}", courseId, pathId);

        LearningPath learningPath = learningPathRepository.findByIdAndIsActive(pathId, Boolean.TRUE)
                .orElseThrow(() -> new RuntimeException("Learning path not found with ID: " + pathId));

        learningPathCourseRepository.deleteByPathIdAndCourseId(pathId, courseId);

        // Refresh to get updated courses
        learningPath = learningPathRepository.findById(pathId).orElseThrow();

        log.info("Course removed successfully from learning path ID: {}", pathId);
        return learningPathMapper.toDTO(learningPath);
    }

    @Override
    public LearningPathResponseDTO reorderCourses(UUID pathId, List<CourseInPathDTO> courses) {
        log.info("=".repeat(80));
        log.info("🔄 REORDER COURSES - START");
        log.info("Reordering courses in learning path ID: {}", pathId);
        log.info("📥 Received {} courses to save", courses.size());

        LearningPath learningPath = learningPathRepository.findByIdAndIsActive(pathId, Boolean.TRUE)
                .orElseThrow(() -> new RuntimeException("Learning path not found with ID: " + pathId));

        // Delete existing courses
        log.info("🗑️ Deleting existing courses for pathId: {}", pathId);
        learningPathCourseRepository.deleteByPathId(pathId);
        log.info("✅ Existing courses deleted");

        // Add courses with new order
        log.info("➕ Adding {} courses with new positions", courses.size());
        for (int i = 0; i < courses.size(); i++) {
            CourseInPathDTO courseDTO = courses.get(i);
            log.info("\n📋 Processing course {}/{}: courseId={}, order={}, positionX={}, positionY={}, isOptional={}",
                    (i + 1), courses.size(),
                    courseDTO.getCourseId(), courseDTO.getOrder(), courseDTO.getPositionX(),
                    courseDTO.getPositionY(), courseDTO.getIsOptional());

            LearningPathCourse pathCourse = new LearningPathCourse();
            pathCourse.setPathId(pathId);
            pathCourse.setCourseId(courseDTO.getCourseId());
            pathCourse.setOrder(courseDTO.getOrder());
            pathCourse.setPositionX(courseDTO.getPositionX());
            pathCourse.setPositionY(courseDTO.getPositionY());
            pathCourse.setIsOptional(courseDTO.getIsOptional() != null ? courseDTO.getIsOptional() : "N");

            log.info(
                    "   ⚙️ Before save - Entity values: pathId={}, courseId={}, order={}, positionX={}, positionY={}, isOptional={}",
                    pathCourse.getPathId(), pathCourse.getCourseId(), pathCourse.getOrder(),
                    pathCourse.getPositionX(), pathCourse.getPositionY(), pathCourse.getIsOptional());

            LearningPathCourse saved = learningPathCourseRepository.save(pathCourse);

            log.info(
                    "   💾 After save - DB values: pathId={}, courseId={}, order={}, positionX={}, positionY={}, isOptional={}",
                    saved.getPathId(), saved.getCourseId(), saved.getOrder(),
                    saved.getPositionX(), saved.getPositionY(), saved.getIsOptional());

            if (saved.getPositionX() == null || saved.getPositionY() == null) {
                log.error("   ❌ WARNING: Position values are NULL after save!");
                log.error("      Original DTO had: positionX={}, positionY={}",
                        courseDTO.getPositionX(), courseDTO.getPositionY());
            } else {
                log.info("   ✅ Position values saved successfully");
            }
        }

        // Refresh to get updated courses
        log.info("\n🔄 Refreshing learning path to get updated courses...");
        learningPath = learningPathRepository.findById(pathId).orElseThrow();

        log.info("📊 Verifying courses after refresh:");
        if (learningPath.getCourses() != null) {
            for (LearningPathCourse course : learningPath.getCourses()) {
                log.info("   📌 Course from DB: courseId={}, order={}, positionX={}, positionY={}",
                        course.getCourseId(), course.getOrder(),
                        course.getPositionX(), course.getPositionY());
            }
        } else {
            log.warn("   ⚠️ Courses list is NULL after refresh!");
        }

        log.info("✅ Courses reordered successfully in learning path ID: {}", pathId);
        log.info("🔄 REORDER COURSES - END");
        log.info("=".repeat(80));

        LearningPathResponseDTO response = learningPathMapper.toDTO(learningPath);

        log.info("\n📤 Response DTO courses:");
        if (response.getCourses() != null) {
            for (CourseInPathDTO courseDTO : response.getCourses()) {
                log.info("   📦 Course DTO: courseId={}, order={}, positionX={}, positionY={}",
                        courseDTO.getCourseId(), courseDTO.getOrder(),
                        courseDTO.getPositionX(), courseDTO.getPositionY());
            }
        }

        return response;
    }

    @Override
    @Transactional(readOnly = true)
    public List<LearningPathResponseDTO> getLearningPathsByCourse(UUID courseId) {
        log.info("Fetching learning paths containing course ID: {}", courseId);

        List<LearningPathCourse> pathCourses = learningPathCourseRepository.findByCourseId(courseId);
        List<UUID> pathIds = pathCourses.stream()
                .map(LearningPathCourse::getPathId)
                .collect(Collectors.toList());

        List<LearningPath> learningPaths = learningPathRepository.findAllById(pathIds);
        return learningPathMapper.toDTOList(learningPaths);
    }

    private void mapSkillsToPath(LearningPath learningPath, List<String> skillNames) {
        log.info("========== mapSkillsToPath START ==========");
        log.info("mapSkillsToPath - Input skillNames: {}", skillNames);
        log.info("mapSkillsToPath - LearningPath ID: {}", learningPath.getId());
        log.info("mapSkillsToPath - Initial path skills count: {}", learningPath.getPathSkills().size());

        // Build a set of skill names to add (normalized)
        java.util.Set<String> requestedSkillNames = new java.util.HashSet<>();
        if (skillNames != null && !skillNames.isEmpty()) {
            for (String name : skillNames) {
                if (name != null && !name.trim().isEmpty()) {
                    requestedSkillNames.add(name.trim());
                }
            }
        }
        log.info("mapSkillsToPath: Requested skill names (normalized): {}", requestedSkillNames);

        // Remove skills that are not in the requested list
        java.util.Iterator<LearningPathSkill> iterator = learningPath.getPathSkills().iterator();
        while (iterator.hasNext()) {
            LearningPathSkill ps = iterator.next();
            String existingSkillName = ps.getSkill() != null ? ps.getSkill().getName() : null;
            if (existingSkillName == null || !requestedSkillNames.contains(existingSkillName)) {
                log.info("mapSkillsToPath: Removing skill: {}", existingSkillName);
                iterator.remove();
            } else {
                // Skill already exists, remove from requested set to avoid duplicate
                log.info("mapSkillsToPath: Skill '{}' already exists, skipping", existingSkillName);
                requestedSkillNames.remove(existingSkillName);
            }
        }
        log.info("mapSkillsToPath: After cleanup, path skills count: {}", learningPath.getPathSkills().size());
        log.info("mapSkillsToPath: Skills to add: {}", requestedSkillNames);

        // Add new skills that don't exist yet
        for (String skillName : requestedSkillNames) {
            log.info("mapSkillsToPath: Processing new skill: '{}'", skillName);

            Skill skill = skillRepository.findByName(skillName)
                    .orElseGet(() -> {
                        log.info("mapSkillsToPath: Skill '{}' not found, creating new", skillName);
                        Skill newSkill = new Skill();
                        newSkill.setName(skillName);
                        Skill saved = skillRepository.save(newSkill);
                        log.info("mapSkillsToPath: Created skill ID: {}, name: '{}'", saved.getId(), saved.getName());
                        return saved;
                    });

            log.info("mapSkillsToPath: Adding skill {} (ID: {}) to path", skill.getName(), skill.getId());

            LearningPathSkill pathSkill = new LearningPathSkill();
            pathSkill.setLearningPath(learningPath);
            pathSkill.setSkill(skill);
            pathSkill.setAssignedAt(OffsetDateTime.now());
            learningPath.getPathSkills().add(pathSkill);
            log.info("mapSkillsToPath: Added LearningPathSkill for skill: {}", skill.getName());
        }

        log.info("mapSkillsToPath: Final path skills count: {}", learningPath.getPathSkills().size());
        log.info("========== mapSkillsToPath END ==========");
    }
}
