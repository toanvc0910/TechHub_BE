# Manage Course Comments — Single Sequence Diagram

This single diagram consolidates Comments For The Course flows in course-service based on:
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
    participant CR as CommentRepository

    alt Get course comments GET /api/courses/{courseId}/comments
        C->>G: GET .../courses/{courseId}/comments
        G->>ComCtrl: GET /api/courses/{courseId}/comments
        ComCtrl->>ComSvc: getCourseComments with course
        ComSvc->>CoR: findById by course id
        CoR-->>ComSvc: Course or error
        ComSvc->>CR: findAllByTarget with course id and target COURSE
        CR-->>ComSvc: list of Comment
        ComSvc->>ComSvc: build comment tree
        ComSvc-->>ComCtrl: list of CommentResponse
        ComCtrl-->>G: 200 OK GlobalResponse
        G-->>C: 200
    else Add course comment POST /api/courses/{courseId}/comments
        C->>G: POST .../courses/{courseId}/comments with CommentRequest
        G->>ComCtrl: POST /api/courses/{courseId}/comments
        ComCtrl->>ComSvc: addCourseComment with course and request
        ComSvc->>CoR: findById by course id
        CoR-->>ComSvc: Course or error
        alt parentId provided
            ComSvc->>CR: findByIdAndIsActiveTrue by parent id
            CR-->>ComSvc: Parent comment or error
            ComSvc->>ComSvc: ensure parent target matches course
        end
        ComSvc->>ComSvc: require current user
        ComSvc->>ComSvc: create Comment entity for target COURSE
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
        ComSvc->>ComSvc: validate comment belongs to specified course
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

