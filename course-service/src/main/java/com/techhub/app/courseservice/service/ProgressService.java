package com.techhub.app.courseservice.service;

import com.techhub.app.courseservice.dto.CreateProgressDTO;
import com.techhub.app.courseservice.dto.ProgressDTO;
import com.techhub.app.courseservice.exception.ResourceNotFoundException;
import com.techhub.app.courseservice.model.Lesson;
import com.techhub.app.courseservice.model.Progress;
import com.techhub.app.courseservice.repository.LessonRepository;
import com.techhub.app.courseservice.repository.ProgressRepository;
import com.techhub.app.courseservice.utils.MapperUtil;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class ProgressService {

    private final ProgressRepository progressRepository;
    private final LessonRepository lessonRepository;
    private final MapperUtil mapperUtil;

    public ProgressService(ProgressRepository progressRepository, LessonRepository lessonRepository, MapperUtil mapperUtil) {
        this.progressRepository = progressRepository;
        this.lessonRepository = lessonRepository;
        this.mapperUtil = mapperUtil;
    }

    @Transactional
    public ProgressDTO createProgress(CreateProgressDTO createProgressDTO) {
        Lesson lesson = lessonRepository.findById(createProgressDTO.getLessonId())
                .orElseThrow(() -> new ResourceNotFoundException("Lesson", "id", createProgressDTO.getLessonId()));

        Progress progress = progressRepository.findByUserIdAndLessonId(
                createProgressDTO.getUserId(), createProgressDTO.getLessonId())
                .orElse(new Progress());

        progress.setUserId(createProgressDTO.getUserId());
        progress.setLesson(lesson);
        progress.setCompletion(createProgressDTO.getCompletion());
        progress.setIsActive(true);

        if (createProgressDTO.getCompletion() >= 100.0) {
            progress.setCompletedAt(LocalDateTime.now());
        }

        Progress savedProgress = progressRepository.save(progress);
        ProgressDTO dto = mapperUtil.map(savedProgress, ProgressDTO.class);
        dto.setLessonId(lesson.getId());
        return dto;
    }

    public ProgressDTO getProgressById(UUID id) {
        Progress progress = progressRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Progress", "id", id));
        ProgressDTO dto = mapperUtil.map(progress, ProgressDTO.class);
        dto.setLessonId(progress.getLesson().getId());
        return dto;
    }

    public List<ProgressDTO> getProgressByUser(UUID userId) {
        List<Progress> progresses = progressRepository.findByUserId(userId);
        return mapperUtil.mapList(progresses, ProgressDTO.class);
    }

    public ProgressDTO getProgressByUserAndLesson(UUID userId, UUID lessonId) {
        Progress progress = progressRepository.findByUserIdAndLessonId(userId, lessonId)
                .orElseThrow(() -> new ResourceNotFoundException("Progress not found for user and lesson"));
        ProgressDTO dto = mapperUtil.map(progress, ProgressDTO.class);
        dto.setLessonId(progress.getLesson().getId());
        return dto;
    }

    @Transactional
    public ProgressDTO updateProgress(UUID id, CreateProgressDTO updateProgressDTO) {
        Progress progress = progressRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Progress", "id", id));

        if (updateProgressDTO.getCompletion() != null) {
            progress.setCompletion(updateProgressDTO.getCompletion());
            if (updateProgressDTO.getCompletion() >= 100.0 && progress.getCompletedAt() == null) {
                progress.setCompletedAt(LocalDateTime.now());
            }
        }

        Progress updatedProgress = progressRepository.save(progress);
        ProgressDTO dto = mapperUtil.map(updatedProgress, ProgressDTO.class);
        dto.setLessonId(updatedProgress.getLesson().getId());
        return dto;
    }

    @Transactional
    public void deleteProgress(UUID id) {
        Progress progress = progressRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Progress", "id", id));
        progress.setIsActive(false);
        progressRepository.save(progress);
    }
}

