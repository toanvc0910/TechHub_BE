package com.techhub.app.courseservice.service;

import com.techhub.app.courseservice.dto.CreateLessonDTO;
import com.techhub.app.courseservice.dto.LessonDTO;
import com.techhub.app.courseservice.exception.ResourceNotFoundException;
import com.techhub.app.courseservice.model.Chapter;
import com.techhub.app.courseservice.model.Lesson;
import com.techhub.app.courseservice.repository.ChapterRepository;
import com.techhub.app.courseservice.repository.LessonRepository;
import com.techhub.app.courseservice.utils.MapperUtil;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class LessonService {

    private final LessonRepository lessonRepository;
    private final ChapterRepository chapterRepository;
    private final MapperUtil mapperUtil;

    public LessonService(LessonRepository lessonRepository, ChapterRepository chapterRepository, MapperUtil mapperUtil) {
        this.lessonRepository = lessonRepository;
        this.chapterRepository = chapterRepository;
        this.mapperUtil = mapperUtil;
    }

    @Transactional
    public LessonDTO createLesson(CreateLessonDTO createLessonDTO) {
        Chapter chapter = chapterRepository.findById(createLessonDTO.getChapterId())
                .orElseThrow(() -> new ResourceNotFoundException("Chapter", "id", createLessonDTO.getChapterId()));

        Lesson lesson = new Lesson();
        lesson.setTitle(createLessonDTO.getTitle());
        lesson.setOrder(createLessonDTO.getOrder());
        lesson.setChapter(chapter);
        lesson.setContentType(createLessonDTO.getContentType());
        lesson.setVideoUrl(createLessonDTO.getVideoUrl());
        lesson.setDocumentUrls(createLessonDTO.getDocumentUrls());
        lesson.setIsActive(true);

        Lesson savedLesson = lessonRepository.save(lesson);
        LessonDTO dto = mapperUtil.map(savedLesson, LessonDTO.class);
        dto.setChapterId(chapter.getId());
        return dto;
    }

    public LessonDTO getLessonById(UUID id) {
        Lesson lesson = lessonRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Lesson", "id", id));
        LessonDTO dto = mapperUtil.map(lesson, LessonDTO.class);
        dto.setChapterId(lesson.getChapter().getId());
        return dto;
    }

    public List<LessonDTO> getAllLessons() {
        List<Lesson> lessons = lessonRepository.findAll();
        return mapperUtil.mapList(lessons, LessonDTO.class);
    }

    public List<LessonDTO> getLessonsByChapter(UUID chapterId) {
        List<Lesson> lessons = lessonRepository.findByChapterIdOrderByOrderAsc(chapterId);
        return mapperUtil.mapList(lessons, LessonDTO.class);
    }

    @Transactional
    public LessonDTO updateLesson(UUID id, CreateLessonDTO updateLessonDTO) {
        Lesson lesson = lessonRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Lesson", "id", id));

        if (updateLessonDTO.getTitle() != null) {
            lesson.setTitle(updateLessonDTO.getTitle());
        }
        if (updateLessonDTO.getOrder() != null) {
            lesson.setOrder(updateLessonDTO.getOrder());
        }
        if (updateLessonDTO.getContentType() != null) {
            lesson.setContentType(updateLessonDTO.getContentType());
        }
        if (updateLessonDTO.getVideoUrl() != null) {
            lesson.setVideoUrl(updateLessonDTO.getVideoUrl());
        }
        if (updateLessonDTO.getDocumentUrls() != null) {
            lesson.setDocumentUrls(updateLessonDTO.getDocumentUrls());
        }

        Lesson updatedLesson = lessonRepository.save(lesson);
        LessonDTO dto = mapperUtil.map(updatedLesson, LessonDTO.class);
        dto.setChapterId(updatedLesson.getChapter().getId());
        return dto;
    }

    @Transactional
    public void deleteLesson(UUID id) {
        Lesson lesson = lessonRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Lesson", "id", id));
        lesson.setIsActive(false);
        lessonRepository.save(lesson);
    }
}

