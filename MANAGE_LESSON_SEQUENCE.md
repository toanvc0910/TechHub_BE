# Manage Lesson — Single Sequence Diagram

This single diagram consolidates Manage Lesson flows in course-service based on:
- service/impl/CourseServiceImpl.java (lesson operations inside course/chapter context)
- repository/LessonRepository.java
- DTOs: LessonRequest, LessonResponse, LessonAssetRequest/Response

```mermaid
sequenceDiagram
    autonumber
    participant C as Client
    participant G as API Gateway
    participant CC as CourseController (API surface assumed)
    participant CS as CourseServiceImpl
    participant CR as CourseRepository
    participant ChR as ChapterRepository
    participant LR as LessonRepository
    participant LAR as LessonAssetRepository

    alt Get lesson (GET /api/courses/{courseId}/chapters/{chapterId}/lessons/{lessonId})
        C->>G: GET .../courses/{courseId}/chapters/{chapterId}/lessons/{lessonId}
        G->>CC: GET /api/courses/{courseId}/chapters/{chapterId}/lessons/{lessonId}
        CC->>CS: getLesson(courseId, chapterId, lessonId)
        CS->>CR: getActiveCourse(courseId)
        CS->>ChR: findByIdAndCourse_IdAndIsActiveTrue(chapterId, courseId)
        ChR-->>CS: Chapter or error
        CS->>LR: findByIdAndChapter_IdAndIsActiveTrue(lessonId, chapterId)
        LR-->>CS: Lesson or NotFound
        CS-->>CC: LessonResponse
        CC-->>G: 200 OK
        G-->>C: 200 OK
    else Create lesson (POST /api/courses/{courseId}/chapters/{chapterId}/lessons)
        C->>G: POST .../courses/{courseId}/chapters/{chapterId}/lessons {LessonRequest}
        G->>CC: POST /api/courses/{courseId}/chapters/{chapterId}/lessons
        CC->>CS: createLesson(courseId, chapterId, request)
        CS->>CR: getActiveCourse(courseId)
        CS->>CS: requireCurrentUser() + ensureCanManage(course, userId)
        CS->>ChR: findByIdAndCourse_IdAndIsActiveTrue(chapterId, courseId)
        ChR-->>CS: Chapter
        opt determine orderIndex
            CS->>LR: findMaxOrderIndexByChapterId(chapterId)
            LR-->>CS: maxOrderIndex
            CS->>CS: orderIndex = (max or 0) + 1 if request.orderIndex == null
        end
        CS->>CS: courseMapper.toLessonEntity(request, chapter, userId, orderIndex)
        CS->>LR: save(new Lesson)
        LR-->>CS: Lesson
        CS-->>CC: LessonResponse
        CC-->>G: 200 OK
        G-->>C: 200 OK
    else Update lesson (PUT /api/courses/{courseId}/chapters/{chapterId}/lessons/{lessonId})
        C->>G: PUT .../courses/{courseId}/chapters/{chapterId}/lessons/{lessonId} {LessonRequest}
        G->>CC: PUT /api/courses/{courseId}/chapters/{chapterId}/lessons/{lessonId}
        CC->>CS: updateLesson(courseId, chapterId, lessonId, request)
        CS->>CR: getActiveCourse(courseId)
        CS->>CS: requireCurrentUser() + ensureCanManage(course, userId)
        CS->>ChR: findByIdAndCourse_IdAndIsActiveTrue(chapterId, courseId)
        ChR-->>CS: Chapter
        CS->>LR: findByIdAndChapter_IdAndIsActiveTrue(lessonId, chapterId)
        LR-->>CS: Lesson or NotFound
        CS->>CS: courseMapper.updateLesson(lesson, request, userId)
        CS->>LR: save(updated Lesson)
        LR-->>CS: Lesson
        CS-->>CC: LessonResponse
        CC-->>G: 200 OK
        G-->>C: 200 OK
    else Delete lesson with auto-reorder (DELETE /api/courses/{courseId}/chapters/{chapterId}/lessons/{lessonId})
        C->>G: DELETE .../courses/{courseId}/chapters/{chapterId}/lessons/{lessonId}
        G->>CC: DELETE /api/courses/{courseId}/chapters/{chapterId}/lessons/{lessonId}
        CC->>CS: deleteLesson(courseId, chapterId, lessonId)
        CS->>CR: getActiveCourse(courseId)
        CS->>CS: requireCurrentUser() + ensureCanManage(course, userId)
        CS->>ChR: findByIdAndCourse_IdAndIsActiveTrue(chapterId, courseId)
        ChR-->>CS: Chapter
        CS->>LR: findByIdAndChapter_IdAndIsActiveTrue(lessonId, chapterId)
        LR-->>CS: Lesson or NotFound
        CS->>LR: delete(lesson) // hard delete (cascade assets & progress)
        CS->>LR: findByChapter_IdAndIsActiveTrueOrderByOrderIndexAsc(chapterId)
        LR-->>CS: [remaining Lessons]
        opt reorder remaining lessons (1..N)
            CS->>CS: for each lesson if orderIndex != newIndex → set orderIndex, updatedBy, updated
            CS->>LR: saveAll(updated lessons)
        end
        CS-->>CC: void
        CC-->>G: 200 OK
        G-->>C: 200 OK
    else Add lesson asset (POST /api/courses/{courseId}/chapters/{chapterId}/lessons/{lessonId}/assets)
        C->>G: POST .../courses/{courseId}/chapters/{chapterId}/lessons/{lessonId}/assets {LessonAssetRequest}
        G->>CC: POST /api/courses/{courseId}/chapters/{chapterId}/lessons/{lessonId}/assets
        CC->>CS: addLessonAsset(courseId, chapterId, lessonId, request)
        CS->>CR: getActiveCourse(courseId)
        CS->>ChR: findByIdAndCourse_IdAndIsActiveTrue(chapterId, courseId)
        ChR-->>CS: Chapter
        CS->>LR: findByIdAndChapter_IdAndIsActiveTrue(lessonId, chapterId)
        LR-->>CS: Lesson
        CS->>CS: validate asset type, map to entity
        CS->>LAR: save(new LessonAsset)
        LAR-->>CS: LessonAsset
        CS-->>CC: LessonAssetResponse
        CC-->>G: 200 OK
        G-->>C: 200 OK
    end
```

