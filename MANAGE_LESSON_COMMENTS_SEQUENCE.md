# Manage Lesson Comments — Single Sequence Diagram

This single diagram consolidates Comment For The Lesson flows in course-service based on:
- controller/CourseCommentController.java
- service/CourseCommentService.java and service/impl/CourseCommentServiceImpl.java

```mermaid
sequenceDiagram
    autonumber
    participant C as Client
    participant G as API Gateway
    participant ComCtrl as CourseCommentController
    participant ComSvc as CourseCommentServiceImpl
    participant CoR as CourseRepository
    participant LR as LessonRepository
    participant CR as CommentRepository

    alt Get lesson comments GET /api/courses/{courseId}/lessons/{lessonId}/comments
        C->>G: GET .../courses/{courseId}/lessons/{lessonId}/comments
        G->>ComCtrl: GET /api/courses/{courseId}/lessons/{lessonId}/comments
        ComCtrl->>ComSvc: getLessonComments with course and lesson
        ComSvc->>LR: findById by lesson id
        LR-->>ComSvc: Lesson or error
        ComSvc->>ComSvc: validate lesson belongs to course and active
        ComSvc->>CR: findAllByTarget with lesson id and target LESSON
        CR-->>ComSvc: list of Comment
        ComSvc->>ComSvc: build comment tree
        ComSvc-->>ComCtrl: list of CommentResponse
        ComCtrl-->>G: 200 OK GlobalResponse
        G-->>C: 200
    else Add lesson comment POST /api/courses/{courseId}/lessons/{lessonId}/comments
        C->>G: POST .../courses/{courseId}/lessons/{lessonId}/comments with CommentRequest
        G->>ComCtrl: POST /api/courses/{courseId}/lessons/{lessonId}/comments
        ComCtrl->>ComSvc: addLessonComment with course lesson and request
        ComSvc->>LR: findById by lesson id
        LR-->>ComSvc: Lesson or error
        ComSvc->>ComSvc: validate lesson belongs to course and active
        alt parentId provided
            ComSvc->>CR: findByIdAndIsActiveTrue by parent id
            CR-->>ComSvc: Parent comment or error
            ComSvc->>ComSvc: ensure parent target matches lesson
        end
        ComSvc->>ComSvc: require current user
        ComSvc->>ComSvc: create Comment entity for target LESSON
        ComSvc->>CR: save comment
        CR-->>ComSvc: Comment saved
        ComSvc-->>ComCtrl: CommentResponse
        ComCtrl-->>G: 200 OK GlobalResponse
        G-->>C: 200
    else Get code comments GET /api/courses/{courseId}/lessons/{lessonId}/workspace/comments
        C->>G: GET .../courses/{courseId}/lessons/{lessonId}/workspace/comments
        G->>ComCtrl: GET /api/courses/{courseId}/lessons/{lessonId}/workspace/comments
        ComCtrl->>ComSvc: getCodeComments with course and lesson
        ComSvc->>LR: findById by lesson id
        LR-->>ComSvc: Lesson or error
        ComSvc->>ComSvc: validate lesson belongs to course and active
        ComSvc->>CR: findAllByTarget with lesson id and target CODE
        CR-->>ComSvc: list of Comment
        ComSvc->>ComSvc: build comment tree
        ComSvc-->>ComCtrl: list of CommentResponse
        ComCtrl-->>G: 200 OK GlobalResponse
        G-->>C: 200
    else Add code comment POST /api/courses/{courseId}/lessons/{lessonId}/workspace/comments
        C->>G: POST .../courses/{courseId}/lessons/{lessonId}/workspace/comments with CommentRequest
        G->>ComCtrl: POST /api/courses/{courseId}/lessons/{lessonId}/workspace/comments
        ComCtrl->>ComSvc: addCodeComment with course lesson and request
        ComSvc->>LR: findById by lesson id
        LR-->>ComSvc: Lesson or error
        ComSvc->>ComSvc: validate lesson belongs to course and active
        alt parentId provided
            ComSvc->>CR: findByIdAndIsActiveTrue by parent id
            CR-->>ComSvc: Parent comment or error
            ComSvc->>ComSvc: ensure parent target matches code workspace
        end
        ComSvc->>ComSvc: require current user
        ComSvc->>ComSvc: create Comment entity for target CODE
        ComSvc->>CR: save comment
        CR-->>ComSvc: Comment saved
        ComSvc-->>ComCtrl: CommentResponse
        ComCtrl-->>G: 200 OK GlobalResponse
        G-->>C: 200
    else Delete comment DELETE /api/courses/{courseId}/comments/{commentId}
        C->>G: DELETE .../courses/{courseId}/comments/{commentId}
        G->>ComCtrl: DELETE /api/courses/{courseId}/comments/{commentId}
        ComCtrl->>ComSvc: deleteComment with course and comment
        ComSvc->>CR: findByIdAndIsActiveTrue by comment id
        CR-->>ComSvc: Comment or error
        ComSvc->>CoR: findById by course id
        CoR-->>ComSvc: Course or error
        ComSvc->>ComSvc: validate comment belongs to course or lesson in course
        ComSvc->>ComSvc: require current user
        ComSvc->>ComSvc: check owner or admin or instructor
        ComSvc->>ComSvc: set isActive false and update fields
        ComSvc->>CR: save comment
        CR-->>ComSvc: Comment saved
        ComSvc-->>ComCtrl: void
        ComCtrl-->>G: 200 OK GlobalResponse
        G-->>C: 200
    end
```

