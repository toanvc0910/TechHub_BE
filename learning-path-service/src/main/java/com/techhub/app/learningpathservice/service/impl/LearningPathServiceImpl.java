package com.techhub.app.learningpathservice.service.impl;

import com.techhub.app.commonservice.context.UserContext;
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
import com.techhub.app.commonservice.kafka.event.LearningPathEventPayload;
import com.techhub.app.commonservice.kafka.publisher.CourseEventPublisher;
import com.techhub.app.learningpathservice.service.LearningPathService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
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
    private final CourseEventPublisher courseEventPublisher;

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public LearningPathResponseDTO createLearningPath(LearningPathRequestDTO requestDTO) {
        log.info("Creating new learning path with title: {}", requestDTO.getTitle());

        LearningPath learningPath = learningPathMapper.toEntity(requestDTO);

        // Map skills to learning path
        if (requestDTO.getSkills() != null) {
            mapSkillsToPath(learningPath, requestDTO.getSkills());
        }

        boolean hasCourses = requestDTO.getCourses() != null && !requestDTO.getCourses().isEmpty();
        learningPath = hasCourses
                ? learningPathRepository.saveAndFlush(learningPath)
                : learningPathRepository.save(learningPath);

        if (hasCourses) {
            List<LearningPathCourse> createdCourses = persistNewPathCourses(
                    learningPath.getId(),
                    requestDTO.getCourses(),
                    new HashSet<>());
            learningPath.getCourses().addAll(createdCourses);
            entityManager.flush();
            log.info("Created learning path {} with {} courses in the same transaction",
                    learningPath.getId(), createdCourses.size());
        }

        publishPathEvent(learningPath, "CREATED");
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

        publishPathEvent(learningPath, "UPDATED");
        log.info("Learning path updated successfully with ID: {}", id);
        return learningPathMapper.toDTO(learningPath);
    }

    @Override
    @Transactional(readOnly = true)
    public LearningPathResponseDTO getLearningPathById(UUID id) {
        log.info("=".repeat(80));
        log.info("GET LEARNING PATH BY ID - START");
        log.info("Fetching learning path with ID: {}", id);

        LearningPath learningPath = learningPathRepository.findByIdAndIsActive(id, Boolean.TRUE)
                .orElseThrow(() -> new RuntimeException("Learning path not found with ID: " + id));

        log.info("Learning path found: title={}, courses count={}",
                learningPath.getTitle(),
                learningPath.getCourses() != null ? learningPath.getCourses().size() : 0);

        if (learningPath.getCourses() != null) {
            log.info("\nCourses from database:");
            for (LearningPathCourse course : learningPath.getCourses()) {
                log.info("   Course: courseId={}, order={}, positionX={}, positionY={}, isOptional={}",
                        course.getCourseId(), course.getOrder(),
                        course.getPositionX(), course.getPositionY(),
                        course.getIsOptional());
            }
        }

        LearningPathResponseDTO response = learningPathMapper.toDTO(learningPath);

        log.info("\nResponse DTO:");
        if (response.getCourses() != null) {
            for (CourseInPathDTO courseDTO : response.getCourses()) {
                log.info("   Course DTO: courseId={}, order={}, positionX={}, positionY={}, isOptional={}",
                        courseDTO.getCourseId(), courseDTO.getOrder(),
                        courseDTO.getPositionX(), courseDTO.getPositionY(),
                        courseDTO.getIsOptional());
            }
        }

        log.info("GET LEARNING PATH BY ID - END");
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
        log.info("Adding {} courses to learning path ID: {}", requestDTO.getCourses().size(), pathId);

        learningPathRepository.findByIdAndIsActive(pathId, Boolean.TRUE)
                .orElseThrow(() -> new RuntimeException("Learning path not found with ID: " + pathId));

        Set<UUID> existingCourseIds = new HashSet<>(learningPathCourseRepository.findCourseIdsByPathId(pathId));
        List<LearningPathCourse> createdCourses = persistNewPathCourses(pathId, requestDTO.getCourses(), existingCourseIds);
        entityManager.flush();
        entityManager.clear();

        LearningPath learningPath = learningPathRepository.findById(pathId).orElseThrow();

        log.info("Courses operation completed for learning path ID: {} - Added: {}, Skipped: {}",
                pathId, createdCourses.size(), requestDTO.getCourses().size() - createdCourses.size());
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
        log.info("Reordering {} courses in learning path ID: {}", courses.size(), pathId);

        learningPathRepository.findByIdAndIsActive(pathId, Boolean.TRUE)
                .orElseThrow(() -> new RuntimeException("Learning path not found with ID: " + pathId));

        learningPathCourseRepository.deleteByPathId(pathId);
        entityManager.flush();

        List<LearningPathCourse> savedCourses = persistNewPathCourses(pathId, courses, new HashSet<>());
        entityManager.flush();
        entityManager.clear();

        LearningPath learningPath = learningPathRepository.findById(pathId).orElseThrow();

        log.info("Courses reordered successfully in learning path ID: {} - Saved: {}, Skipped: {}",
                pathId, savedCourses.size(), courses.size() - savedCourses.size());
        return learningPathMapper.toDTO(learningPath);
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
        Set<String> requestedSkillNames = skillNames == null
                ? new LinkedHashSet<>()
                : skillNames.stream()
                        .filter(Objects::nonNull)
                        .map(String::trim)
                        .filter(name -> !name.isEmpty())
                        .collect(Collectors.toCollection(LinkedHashSet::new));

        if (requestedSkillNames.isEmpty()) {
            learningPath.getPathSkills().clear();
            log.info("mapSkillsToPath completed for path {} - requested=0, final=0", learningPath.getId());
            return;
        }

        Set<String> existingSkillNames = learningPath.getPathSkills().stream()
                .map(pathSkill -> pathSkill.getSkill() != null ? pathSkill.getSkill().getName() : null)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        learningPath.getPathSkills().removeIf(pathSkill -> {
            String existingSkillName = pathSkill.getSkill() != null ? pathSkill.getSkill().getName() : null;
            return existingSkillName == null || !requestedSkillNames.contains(existingSkillName);
        });

        requestedSkillNames.removeAll(existingSkillNames);
        if (requestedSkillNames.isEmpty()) {
            log.info("mapSkillsToPath completed for path {} - no new skills, final={}",
                    learningPath.getId(), learningPath.getPathSkills().size());
            return;
        }

        Map<String, Skill> skillsByName = skillRepository.findByNameIn(requestedSkillNames).stream()
                .collect(Collectors.toMap(Skill::getName, Function.identity()));

        UUID currentUserId = UserContext.getCurrentUserId();
        List<Skill> missingSkills = requestedSkillNames.stream()
                .filter(skillName -> !skillsByName.containsKey(skillName))
                .map(skillName -> {
                    Skill skill = new Skill();
                    skill.setName(skillName);
                    skill.setCreatedBy(currentUserId);
                    skill.setUpdatedBy(currentUserId);
                    return skill;
                })
                .collect(Collectors.toList());

        if (!missingSkills.isEmpty()) {
            skillRepository.saveAll(missingSkills).forEach(skill -> skillsByName.put(skill.getName(), skill));
        }

        OffsetDateTime assignedAt = OffsetDateTime.now();
        for (String skillName : requestedSkillNames) {
            Skill skill = skillsByName.get(skillName);
            if (skill == null) {
                continue;
            }

            LearningPathSkill pathSkill = new LearningPathSkill();
            pathSkill.setLearningPath(learningPath);
            pathSkill.setSkill(skill);
            pathSkill.setAssignedAt(assignedAt);
            learningPath.getPathSkills().add(pathSkill);
        }

        log.info("mapSkillsToPath completed for path {} - added={}, final={}",
                learningPath.getId(), requestedSkillNames.size(), learningPath.getPathSkills().size());
    }

    private List<LearningPathCourse> persistNewPathCourses(
            UUID pathId,
            List<CourseInPathDTO> courses,
            Set<UUID> existingCourseIds) {
        List<LearningPathCourse> createdCourses = new ArrayList<>();
        if (courses == null || courses.isEmpty()) {
            return createdCourses;
        }

        LearningPath pathRef = entityManager.getReference(LearningPath.class, pathId);
        for (CourseInPathDTO courseDTO : courses) {
            UUID courseId = courseDTO.getCourseId();
            if (courseId == null || !existingCourseIds.add(courseId)) {
                continue;
            }

            LearningPathCourse pathCourse = new LearningPathCourse();
            pathCourse.setPathId(pathId);
            pathCourse.setCourseId(courseId);
            pathCourse.setOrder(courseDTO.getOrder());
            pathCourse.setPositionX(courseDTO.getPositionX());
            pathCourse.setPositionY(courseDTO.getPositionY());
            pathCourse.setIsOptional(courseDTO.getIsOptional() != null ? courseDTO.getIsOptional() : "N");
            pathCourse.setLearningPath(pathRef);

            entityManager.persist(pathCourse);
            createdCourses.add(pathCourse);
        }
        return createdCourses;
    }

    private void publishPathEvent(LearningPath path, String eventType) {
        try {
            int courseCount = path.getCourses() != null ? path.getCourses().size() : 0;
            courseEventPublisher.publishLearningPathEvent(LearningPathEventPayload.builder()
                    .eventType(eventType)
                    .pathId(String.valueOf(path.getId()))
                    .title(path.getTitle())
                    .description(path.getDescription())
                    .courseCount(courseCount)
                    .build());
        } catch (Exception e) {
            log.warn("Failed to publish learning path event for path {}: {}", path.getId(), e.getMessage());
        }
    }
}

