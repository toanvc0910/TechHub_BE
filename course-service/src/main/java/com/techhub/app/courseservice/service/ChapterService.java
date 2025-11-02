package com.techhub.app.courseservice.service;

import com.techhub.app.courseservice.dto.ChapterDTO;
import com.techhub.app.courseservice.dto.CreateChapterDTO;
import com.techhub.app.courseservice.exception.ResourceNotFoundException;
import com.techhub.app.courseservice.model.Chapter;
import com.techhub.app.courseservice.model.Course;
import com.techhub.app.courseservice.repository.ChapterRepository;
import com.techhub.app.courseservice.repository.CourseRepository;
import com.techhub.app.courseservice.utils.MapperUtil;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class ChapterService {

    private final ChapterRepository chapterRepository;
    private final CourseRepository courseRepository;
    private final MapperUtil mapperUtil;

    public ChapterService(ChapterRepository chapterRepository, CourseRepository courseRepository, MapperUtil mapperUtil) {
        this.chapterRepository = chapterRepository;
        this.courseRepository = courseRepository;
        this.mapperUtil = mapperUtil;
    }

    @Transactional
    public ChapterDTO createChapter(CreateChapterDTO createChapterDTO) {
        Course course = courseRepository.findById(createChapterDTO.getCourseId())
                .orElseThrow(() -> new ResourceNotFoundException("Course", "id", createChapterDTO.getCourseId()));

        Chapter chapter = new Chapter();
        chapter.setTitle(createChapterDTO.getTitle());
        chapter.setOrder(createChapterDTO.getOrder());
        chapter.setCourse(course);
        chapter.setIsActive(true);

        Chapter savedChapter = chapterRepository.save(chapter);
        ChapterDTO dto = mapperUtil.map(savedChapter, ChapterDTO.class);
        dto.setCourseId(course.getId());
        return dto;
    }

    public ChapterDTO getChapterById(UUID id) {
        Chapter chapter = chapterRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Chapter", "id", id));
        ChapterDTO dto = mapperUtil.map(chapter, ChapterDTO.class);
        dto.setCourseId(chapter.getCourse().getId());
        return dto;
    }

    public List<ChapterDTO> getAllChapters() {
        List<Chapter> chapters = chapterRepository.findAll();
        return mapperUtil.mapList(chapters, ChapterDTO.class);
    }

    public List<ChapterDTO> getChaptersByCourse(UUID courseId) {
        List<Chapter> chapters = chapterRepository.findByCourseIdOrderByOrderAsc(courseId);
        return mapperUtil.mapList(chapters, ChapterDTO.class);
    }

    @Transactional
    public ChapterDTO updateChapter(UUID id, CreateChapterDTO updateChapterDTO) {
        Chapter chapter = chapterRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Chapter", "id", id));

        if (updateChapterDTO.getTitle() != null) {
            chapter.setTitle(updateChapterDTO.getTitle());
        }
        if (updateChapterDTO.getOrder() != null) {
            chapter.setOrder(updateChapterDTO.getOrder());
        }

        Chapter updatedChapter = chapterRepository.save(chapter);
        ChapterDTO dto = mapperUtil.map(updatedChapter, ChapterDTO.class);
        dto.setCourseId(updatedChapter.getCourse().getId());
        return dto;
    }

    @Transactional
    public void deleteChapter(UUID id) {
        Chapter chapter = chapterRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Chapter", "id", id));
        chapter.setIsActive(false);
        chapterRepository.save(chapter);
    }
}

