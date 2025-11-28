# Manage Chapters — Single Sequence Diagram

This single diagram consolidates Manage Chapters flows in course-service based on:
- service/impl/CourseServiceImpl.java
- repository/ChapterRepository.java

```mermaid
sequenceDiagram
    autonumber
    participant C as Client
    participant G as API Gateway
    participant CC as CourseController (API surface assumed)
    participant CS as CourseServiceImpl
    participant CR as CourseRepository
    participant ChR as ChapterRepository
    participant PR as ProgressRepository

    alt List chapters of a course (GET /api/courses/{courseId}/chapters)
        C->>G: GET .../courses/{courseId}/chapters
        G->>CC: GET /api/courses/{courseId}/chapters
        CC->>CS: getChapters(courseId)
        CS->>CR: getActiveCourse(courseId)
        CS->>CS: currentUserId = UserContext.getCurrentUserId()
        CS->>CS: check published or manager (canManageCourse)
        alt not published and not manager
            CS-->>CC: NotFound error
            CC-->>G: 404 Not Found
            G-->>C: 404
        else allowed
            CS->>PR: findByUserAndCourse(currentUserId, courseId) (if userId exists)
            PR-->>CS: [Progress]
            CS-->>CC: List<ChapterResponse>
            CC-->>G: 200 OK
            G-->>C: 200 OK
        end
    else Get course detail (GET /api/courses/{courseId}) including chapter snapshot
        C->>G: GET .../courses/{courseId}
        G->>CC: GET /api/courses/{courseId}
        CC->>CS: getCourse(courseId)
        CS->>CR: getActiveCourse(courseId)
        CS->>CS: determine manager/published
        CS->>PR: findByUserAndCourse(currentUserId, courseId) (if user)
        PR-->>CS: [Progress]
        CS-->>CC: CourseDetailResponse{chapters, totals, progress}
        CC-->>G: 200 OK
        G-->>C: 200 OK
    else Create chapter (POST /api/courses/{courseId}/chapters)
        C->>G: POST .../courses/{courseId}/chapters {ChapterRequest}
        G->>CC: POST /api/courses/{courseId}/chapters
        CC->>CS: createChapter(courseId, request)
        CS->>CR: getActiveCourse(courseId)
        CS->>CS: requireCurrentUser() + ensureCanManage(course, userId)
        opt determine orderIndex
            CS->>ChR: findMaxOrderIndexByCourseId(courseId)
            ChR-->>CS: maxOrderIndex
            CS->>CS: nextOrder = (max or 0) + 1 if request.orderIndex == null
        end
        CS->>CS: courseMapper.toChapterEntity(request, course, userId, nextOrder)
        CS->>ChR: save(new Chapter)
        ChR-->>CS: Chapter
        CS-->>CC: ChapterResponse (manager view)
        CC-->>G: 200 OK
        G-->>C: 200 OK
    else Update chapter (PUT /api/courses/{courseId}/chapters/{chapterId})
        C->>G: PUT .../courses/{courseId}/chapters/{chapterId} {ChapterRequest}
        G->>CC: PUT /api/courses/{courseId}/chapters/{chapterId}
        CC->>CS: updateChapter(courseId, chapterId, request)
        CS->>CR: getActiveCourse(courseId)
        CS->>CS: requireCurrentUser() + ensureCanManage(course, userId)
        CS->>ChR: findByIdAndCourse_IdAndIsActiveTrue(chapterId, courseId)
        ChR-->>CS: Chapter or NotFound
        CS->>CS: courseMapper.updateChapter(chapter, request, userId)
        CS->>ChR: save(updated Chapter)
        ChR-->>CS: Chapter
        CS-->>CC: ChapterResponse (manager view)
        CC-->>G: 200 OK
        G-->>C: 200 OK
    else Delete chapter with auto-reorder (DELETE /api/courses/{courseId}/chapters/{chapterId})
        C->>G: DELETE .../courses/{courseId}/chapters/{chapterId}
        G->>CC: DELETE /api/courses/{courseId}/chapters/{chapterId}
        CC->>CS: deleteChapter(courseId, chapterId)
        CS->>CR: getActiveCourse(courseId)
        CS->>CS: requireCurrentUser() + ensureCanManage(course, userId)
        CS->>ChR: findByIdAndCourse_IdAndIsActiveTrue(chapterId, courseId)
        ChR-->>CS: Chapter or NotFound
        CS->>ChR: delete(chapter)
        CS->>ChR: findByCourse_IdAndIsActiveTrueOrderByOrderIndexAsc(courseId)
        ChR-->>CS: [remaining Chapters]
        opt reorder remaining chapters (1..N)
            CS->>CS: for each chapter if orderIndex != newIndex → set orderIndex, updatedBy, updated
            CS->>ChR: saveAll(updated chapters)
        end
        CS-->>CC: void
        CC-->>G: 200 OK
        G-->>C: 200 OK
    end
```

