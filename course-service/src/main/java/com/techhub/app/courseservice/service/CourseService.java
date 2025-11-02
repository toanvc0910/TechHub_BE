package com.techhub.app.courseservice.service;

import com.techhub.app.courseservice.dto.request.ChapterRequest;
import com.techhub.app.courseservice.dto.request.CourseRequest;
import com.techhub.app.courseservice.dto.request.LessonAssetRequest;
import com.techhub.app.courseservice.dto.request.LessonRequest;
import com.techhub.app.courseservice.dto.response.ChapterResponse;
import com.techhub.app.courseservice.dto.response.CourseDetailResponse;
import com.techhub.app.courseservice.dto.response.CourseSummaryResponse;
import com.techhub.app.courseservice.dto.response.LessonAssetResponse;
import com.techhub.app.courseservice.dto.response.LessonResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.UUID;

public interface CourseService {

    Page<CourseSummaryResponse> getCourses(String search, Pageable pageable);

    CourseDetailResponse getCourse(UUID courseId);

    CourseDetailResponse createCourse(CourseRequest request);

    CourseDetailResponse updateCourse(UUID courseId, CourseRequest request);

    void deleteCourse(UUID courseId);

    void enrollCourse(UUID courseId);

    List<ChapterResponse> getChapters(UUID courseId);

    ChapterResponse createChapter(UUID courseId, ChapterRequest request);

    ChapterResponse updateChapter(UUID courseId, UUID chapterId, ChapterRequest request);

    void deleteChapter(UUID courseId, UUID chapterId);

    LessonResponse createLesson(UUID courseId, UUID chapterId, LessonRequest request);

    LessonResponse updateLesson(UUID courseId, UUID chapterId, UUID lessonId, LessonRequest request);

    void deleteLesson(UUID courseId, UUID chapterId, UUID lessonId);

    LessonAssetResponse addLessonAsset(UUID courseId, UUID chapterId, UUID lessonId, LessonAssetRequest request);

    LessonAssetResponse updateLessonAsset(UUID courseId, UUID chapterId, UUID lessonId, UUID assetId, LessonAssetRequest request);

    void deleteLessonAsset(UUID courseId, UUID chapterId, UUID lessonId, UUID assetId);
}
