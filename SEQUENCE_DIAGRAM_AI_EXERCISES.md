# TechHub - Sequence Diagram: Generate AI Exercises

## Mục lục

1. [Tổng quan](#1-tổng-quan)
2. [Các thành phần chính](#2-các-thành-phần-chính)
3. [API Endpoints](#3-api-endpoints)
4. [Data Structures](#4-data-structures)
5. [Chi tiết luồng xử lý](#5-chi-tiết-luồng-xử-lý)
6. [Sequence Diagrams](#6-sequence-diagrams)
7. [State Diagrams](#7-state-diagrams)
8. [Error Handling](#8-error-handling)
9. [Business Rules](#9-business-rules)

---

## 1. Tổng quan

Luồng **Generate AI Exercises** cho phép Instructor/Admin tự động tạo bài tập cho lesson bằng AI (OpenAI GPT). Hệ thống hỗ trợ:

- **Multiple Exercise Types**: MCQ (Multiple Choice), CODING, ESSAY
- **Difficulty Levels**: BEGINNER, INTERMEDIATE, ADVANCED
- **Draft System**: AI tạo draft → Admin review → Approve/Reject
- **Content-Aware**: Sử dụng Vector Search (Qdrant) để lấy nội dung lesson

### Workflow tổng quan

```
Instructor Request → AI Generate Draft → Admin Review → Approve/Reject → Save to Course Service
```

---

## 2. Các thành phần chính

| Component                    | Service        | Vai trò                                             |
| ---------------------------- | -------------- | --------------------------------------------------- |
| `AiExerciseController`       | ai-service     | Endpoint API nhận request generate exercises        |
| `AiDraftController`          | ai-service     | Endpoint API quản lý drafts (list, approve, reject) |
| `AiExerciseService`          | ai-service     | Business logic generate exercises từ AI             |
| `AiDraftApprovalService`     | ai-service     | Business logic approve/reject drafts                |
| `VectorService`              | ai-service     | Lấy nội dung lesson từ Qdrant vector database       |
| `OpenAiGateway`              | ai-service     | Client gọi OpenAI GPT API                           |
| `AiGenerationTaskRepository` | ai-service     | CRUD AiGenerationTask entity (lưu drafts)           |
| `AiProxyController`          | proxy-client   | Proxy endpoint cho Frontend gọi AI Service          |
| `ExerciseController`         | course-service | Endpoint CRUD exercises (sau khi approve)           |
| `ExerciseService`            | course-service | Business logic quản lý exercises                    |

---

## 3. API Endpoints

### AI Service Endpoints

| Method | Endpoint                                          | Mô tả                             |
| ------ | ------------------------------------------------- | --------------------------------- |
| POST   | `/api/ai/exercises/generate`                      | Tạo AI exercise drafts cho lesson |
| GET    | `/api/ai/drafts/exercises?lessonId={uuid}`        | Lấy danh sách drafts của lesson   |
| GET    | `/api/ai/drafts/exercises/latest?lessonId={uuid}` | Lấy draft mới nhất                |
| GET    | `/api/ai/drafts/{taskId}`                         | Lấy chi tiết một draft            |
| POST   | `/api/ai/drafts/{taskId}/approve-exercise`        | Approve exercise draft            |
| POST   | `/api/ai/drafts/{taskId}/reject`                  | Reject draft với lý do            |

### Proxy Client Endpoints (qua API Gateway)

| Method | Endpoint                                         | Mô tả                    |
| ------ | ------------------------------------------------ | ------------------------ |
| POST   | `/api/proxy/ai/exercises/generate`               | Proxy generate exercises |
| POST   | `/api/proxy/ai/drafts/{taskId}/approve-exercise` | Proxy approve draft      |

---

## 4. Data Structures

### 4.1 Enums

#### ExerciseFormat

```java
public enum ExerciseFormat {
    MCQ,      // Multiple Choice Question
    ESSAY,    // Essay/Long answer
    CODING    // Coding exercise with test cases
}
```

#### DifficultyLevel

```java
public enum DifficultyLevel {
    BEGINNER,
    INTERMEDIATE,
    ADVANCED
}
```

#### AiTaskStatus

```java
public enum AiTaskStatus {
    DRAFT,      // AI đã tạo xong, chờ admin review
    APPROVED,   // Admin đã approve
    REJECTED,   // Admin từ chối
    PENDING,    // Đang chờ xử lý
    RUNNING,    // Đang chạy
    COMPLETED,  // Hoàn thành
    FAILED      // Thất bại
}
```

#### AiTaskType

```java
public enum AiTaskType {
    EXERCISE_GENERATION,
    LEARNING_PATH_GENERATION,
    CHAT_GENERAL,
    CHAT_ADVISOR,
    // ...
}
```

### 4.2 Request DTOs

#### AiExerciseGenerateRequest

```json
{
  "courseId": "uuid",
  "lessonId": "uuid",
  "language": "vi",
  "difficulties": ["BEGINNER", "INTERMEDIATE"],
  "formats": ["MCQ", "CODING"],
  "variants": 1,
  "includeExplanations": true,
  "includeTestCases": true,
  "customInstruction": "Focus on practical examples",
  "count": 5,
  "type": "MCQ",
  "difficulty": "BEGINNER"
}
```

### 4.3 Response DTOs

#### AiExerciseGenerationResponse

```json
{
  "taskId": "uuid",
  "status": "DRAFT",
  "drafts": {
    "exercises": [
      {
        "type": "MCQ",
        "question": "Đâu là từ khóa khai báo biến trong JavaScript?",
        "options": ["var", "int", "string", "float"],
        "correctAnswer": 0,
        "explanation": "var là từ khóa khai báo biến trong JavaScript..."
      },
      {
        "type": "CODING",
        "question": "Viết hàm tính tổng 2 số",
        "testCases": [
          { "input": "1, 2", "expectedOutput": "3", "hidden": false },
          { "input": "-1, 5", "expectedOutput": "4", "hidden": true }
        ],
        "explanation": "Sử dụng toán tử + để cộng 2 số..."
      }
    ]
  },
  "message": "Exercise draft created successfully. Admin can review and approve."
}
```

#### ExerciseDraft

```json
{
  "type": "MCQ | CODING | ESSAY",
  "question": "Câu hỏi...",
  "options": ["A", "B", "C", "D"],
  "testCases": [
    {
      "input": "input data",
      "expectedOutput": "expected result",
      "hidden": false
    }
  ],
  "explanation": "Giải thích đáp án..."
}
```

#### AiDraftListResponse

```json
{
  "taskId": "uuid",
  "taskType": "EXERCISE_GENERATION",
  "status": "DRAFT",
  "targetReference": "lesson-uuid",
  "resultPayload": { "exercises": [...] },
  "requestPayload": { "courseId": "...", "lessonId": "..." },
  "prompt": "Generate 5 MCQ exercises...",
  "createdAt": "2024-01-15T10:30:00Z"
}
```

#### ApproveExerciseDraftResponse

```json
{
  "taskId": "uuid",
  "exerciseId": "uuid",
  "lessonId": "uuid",
  "message": "Draft approved. Result payload is ready for Course Service API.",
  "success": true
}
```

---

## 5. Chi tiết luồng xử lý

### 5.1 Generate AI Exercises Flow

#### Bước 1: Instructor gửi request

- **Endpoint**: `POST /api/ai/exercises/generate`
- **Authentication**: Required (JWT Token, role INSTRUCTOR/ADMIN)

#### Bước 2: Fetch Lesson Content từ Qdrant

1. Call `VectorService.getLesson(lessonId)`
2. Qdrant trả về lesson title + content đã được index
3. Nếu không tìm thấy → throw error

#### Bước 3: Tạo AI Generation Task

1. Tạo `AiGenerationTask` với:
   - `taskType` = EXERCISE_GENERATION
   - `status` = DRAFT
   - `targetReference` = lessonId (để query sau)
   - `requestPayload` = original request
2. Build prompt từ lesson content + requirements
3. Lưu vào database

#### Bước 4: Gọi OpenAI GPT

1. Call `OpenAiGateway.generateStructuredJson(prompt, request)`
2. OpenAI trả về JSON với array exercises

#### Bước 5: Lưu Draft

1. Update task với `resultPayload` = AI response
2. Keep `status` = DRAFT
3. Return response với taskId

### 5.2 Get Exercise Drafts Flow

#### 5.2.1 Get All Drafts for Lesson

- **Endpoint**: `GET /api/ai/drafts/exercises?lessonId={uuid}`
- Query by `targetReference` + `status=DRAFT` + `taskType=EXERCISE_GENERATION`
- Order by `created DESC`

#### 5.2.2 Get Latest Draft

- **Endpoint**: `GET /api/ai/drafts/exercises/latest?lessonId={uuid}`
- Return first draft matching criteria

#### 5.2.3 Get Draft Detail

- **Endpoint**: `GET /api/ai/drafts/{taskId}`
- Return full draft info including prompt

### 5.3 Approve Exercise Draft Flow

#### Bước 1: Admin request approve

- **Endpoint**: `POST /api/ai/drafts/{taskId}/approve-exercise`
- **Authentication**: Required (role ADMIN/INSTRUCTOR)

#### Bước 2: Validate Task

1. Find task by taskId
2. Validate `taskType` == EXERCISE_GENERATION
3. Validate `status` == DRAFT

#### Bước 3: Update Status

1. Set `status` = APPROVED
2. Save to database

#### Bước 4: Return Response

- Return taskId, lessonId, success message
- Admin sau đó dùng `resultPayload` để tạo exercise trong Course Service

### 5.4 Reject Draft Flow

#### Endpoint

- `POST /api/ai/drafts/{taskId}/reject?reason={string}`

#### Logic

1. Find and validate task
2. Set `status` = REJECTED
3. Set `errorMessage` = reason
4. Save to database

---

## 6. Sequence Diagrams

### 6.1 Generate AI Exercises

```mermaid
sequenceDiagram
    autonumber
    participant Client
    participant APIGateway
    participant AiExerciseController
    participant AiExerciseService
    participant VectorService
    participant QdrantClient
    participant OpenAiGateway
    participant AiTaskRepository as AiGenerationTaskRepository
    participant Qdrant
    participant OpenAI API
    participant Database

    rect rgb(230, 245, 255)
        Note over Client,Database: 1. Generate AI Exercises Flow

        Client->>+APIGateway: POST /api/ai/exercises/generate<br/>Authorization: Bearer {token}<br/>{ courseId, lessonId, type, count, difficulty }
        APIGateway->>APIGateway: Validate JWT Token<br/>Check role: INSTRUCTOR/ADMIN
        APIGateway->>+AiExerciseController: generateExercises(request)
        AiExerciseController->>+AiExerciseService: generateForLesson(request)

        %% Phase 1: Fetch Lesson Content from Qdrant
        rect rgb(255, 248, 220)
            Note over AiExerciseService,Qdrant: Phase 1: Fetch Lesson Content

            AiExerciseService->>+VectorService: getLesson(lessonId)
            VectorService->>+QdrantClient: getPoint(collection="lessons", id=lessonId)
            QdrantClient->>+Qdrant: GET /collections/lessons/points/{lessonId}
            Qdrant-->>-QdrantClient: Point { id, payload: {title, content, ...} }
            QdrantClient-->>-VectorService: Map<String, Object> lessonData
            VectorService-->>-AiExerciseService: { title, content }

            alt Lesson not found in Qdrant
                AiExerciseService-->>AiExerciseController: throw RuntimeException
                AiExerciseController-->>APIGateway: 500 Internal Server Error
                APIGateway-->>Client: "Lesson content not found in AI index.<br/>Please ask admin to reindex lessons."
            end

            AiExerciseService->>AiExerciseService: Extract lessonTitle, lessonContent
        end

        %% Phase 2: Create AI Generation Task
        rect rgb(220, 255, 220)
            Note over AiExerciseService,Database: Phase 2: Create Task & Build Prompt

            AiExerciseService->>AiExerciseService: Create AiGenerationTask<br/>- taskType: EXERCISE_GENERATION<br/>- status: DRAFT<br/>- targetReference: lessonId<br/>- requestPayload: request

            AiExerciseService->>AiExerciseService: buildPrompt(request, title, content)<br/><br/>"Generate {count} {type} exercises<br/>for lesson '{title}'.<br/>Lesson Content: {content}<br/>Difficulty: {difficulty}<br/>Requirements:<br/>1. Return JSON with 'exercises' array<br/>2. Each exercise has: type, question,<br/>   options (MCQ), testCases (CODING),<br/>   explanation"

            AiExerciseService->>AiExerciseService: task.setPrompt(prompt)

            AiExerciseService->>+AiTaskRepository: save(task)
            AiTaskRepository->>Database: INSERT INTO ai_generation_tasks<br/>(task_type, status, target_reference,<br/>request_payload, prompt)
            Database-->>AiTaskRepository: AiGenerationTask with ID
            AiTaskRepository-->>-AiExerciseService: AiGenerationTask
        end

        %% Phase 3: Call OpenAI
        rect rgb(255, 240, 245)
            Note over AiExerciseService,OpenAI API: Phase 3: Generate with OpenAI

            AiExerciseService->>+OpenAiGateway: generateStructuredJson(prompt, request)

            OpenAiGateway->>OpenAiGateway: Build OpenAI request<br/>- model: gpt-4<br/>- messages: [system, user]<br/>- response_format: json_object<br/>- temperature: 0.7

            OpenAiGateway->>+OpenAI API: POST /v1/chat/completions<br/>{ model, messages, response_format }

            alt OpenAI Success
                OpenAI API-->>-OpenAiGateway: ChatCompletion<br/>{ choices: [{ message: { content: JSON } }] }
                OpenAiGateway->>OpenAiGateway: Parse JSON response
                OpenAiGateway-->>-AiExerciseService: Object aiResponse<br/>{ exercises: [...] }
            else OpenAI Error
                OpenAI API-->>OpenAiGateway: Error response
                OpenAiGateway-->>AiExerciseService: throw Exception

                AiExerciseService->>AiExerciseService: task.setStatus(FAILED)<br/>task.setResultPayload({ error: message })
                AiExerciseService->>AiTaskRepository: save(task)
                AiExerciseService-->>AiExerciseController: throw RuntimeException
                AiExerciseController-->>APIGateway: 500 Error
                APIGateway-->>Client: "AI generation failed: {error}"
            end
        end

        %% Phase 4: Save Draft Result
        rect rgb(245, 255, 245)
            Note over AiExerciseService,Database: Phase 4: Save Draft

            AiExerciseService->>AiExerciseService: task.setResultPayload(aiResponse)<br/>task.setStatus(DRAFT)

            AiExerciseService->>+AiTaskRepository: save(task)
            AiTaskRepository->>Database: UPDATE ai_generation_tasks<br/>SET result_payload = ?,<br/>status = 'DRAFT'<br/>WHERE id = ?
            Database-->>AiTaskRepository: Updated
            AiTaskRepository-->>-AiExerciseService: AiGenerationTask

            AiExerciseService->>AiExerciseService: Build response<br/>- taskId: task.id<br/>- status: DRAFT<br/>- drafts: aiResponse<br/>- message: "Exercise draft created..."
        end

        AiExerciseService-->>-AiExerciseController: AiExerciseGenerationResponse
        AiExerciseController-->>-APIGateway: GlobalResponse<br/>status: "AI_EXERCISE_DRAFT"
        APIGateway-->>-Client: 200 OK<br/>{ taskId, status: "DRAFT",<br/>drafts: { exercises: [...] } }
    end
```

### 6.2 Get & Review Drafts

```mermaid
sequenceDiagram
    autonumber
    participant Admin
    participant APIGateway
    participant AiDraftController
    participant AiTaskRepository as AiGenerationTaskRepository
    participant Database

    rect rgb(240, 248, 255)
        Note over Admin,Database: 2.1: Get All Drafts for Lesson

        Admin->>+APIGateway: GET /api/ai/drafts/exercises?lessonId={uuid}
        APIGateway->>+AiDraftController: getExerciseDrafts(lessonId)

        AiDraftController->>+AiTaskRepository: findByTargetReferenceAndStatusAndTaskType<br/>(lessonId, DRAFT, EXERCISE_GENERATION)
        AiTaskRepository->>Database: SELECT * FROM ai_generation_tasks<br/>WHERE target_reference = ?<br/>AND status = 'DRAFT'<br/>AND task_type = 'EXERCISE_GENERATION'<br/>ORDER BY created DESC
        Database-->>AiTaskRepository: List<AiGenerationTask>
        AiTaskRepository-->>-AiDraftController: List<AiGenerationTask>

        AiDraftController->>AiDraftController: Map to AiDraftListResponse<br/>- taskId, taskType, status<br/>- targetReference, resultPayload<br/>- createdAt

        AiDraftController-->>-APIGateway: GlobalResponse<List<AiDraftListResponse>>
        APIGateway-->>-Admin: 200 OK<br/>[{ taskId, status, resultPayload, ... }]
    end

    rect rgb(255, 248, 240)
        Note over Admin,Database: 2.2: Get Latest Draft

        Admin->>+APIGateway: GET /api/ai/drafts/exercises/latest?lessonId={uuid}
        APIGateway->>+AiDraftController: getLatestExerciseDraft(lessonId)

        AiDraftController->>+AiTaskRepository: findFirstByTargetReference...<br/>OrderByCreatedDesc(lessonId, DRAFT, EXERCISE_GENERATION)
        AiTaskRepository->>Database: SELECT * FROM ai_generation_tasks<br/>WHERE target_reference = ?<br/>AND status = 'DRAFT'<br/>AND task_type = 'EXERCISE_GENERATION'<br/>ORDER BY created DESC<br/>LIMIT 1
        Database-->>AiTaskRepository: AiGenerationTask or null
        AiTaskRepository-->>-AiDraftController: Optional<AiGenerationTask>

        alt Draft not found
            AiDraftController-->>APIGateway: throw RuntimeException
            APIGateway-->>Admin: 500 "No draft found for lesson"
        end

        AiDraftController-->>-APIGateway: GlobalResponse<AiDraftListResponse>
        APIGateway-->>-Admin: 200 OK<br/>{ taskId, resultPayload: { exercises: [...] } }
    end

    rect rgb(230, 245, 255)
        Note over Admin,Database: 2.3: Get Draft Detail

        Admin->>+APIGateway: GET /api/ai/drafts/{taskId}
        APIGateway->>+AiDraftController: getDraftById(taskId)

        AiDraftController->>+AiTaskRepository: findById(taskId)
        AiTaskRepository->>Database: SELECT * FROM ai_generation_tasks<br/>WHERE id = ?
        Database-->>AiTaskRepository: AiGenerationTask
        AiTaskRepository-->>-AiDraftController: Optional<AiGenerationTask>

        alt Draft not found
            AiDraftController-->>APIGateway: throw RuntimeException
            APIGateway-->>Admin: 500 "Draft not found"
        end

        AiDraftController->>AiDraftController: Build full response<br/>Include: requestPayload, prompt

        AiDraftController-->>-APIGateway: GlobalResponse<AiDraftListResponse>
        APIGateway-->>-Admin: 200 OK<br/>{ taskId, prompt, requestPayload,<br/>resultPayload: { exercises } }
    end
```

### 6.3 Approve & Reject Drafts

```mermaid
sequenceDiagram
    autonumber
    participant Admin
    participant APIGateway
    participant AiDraftController
    participant AiDraftApprovalService
    participant AiTaskRepository as AiGenerationTaskRepository
    participant Database

    rect rgb(220, 255, 220)
        Note over Admin,Database: 3.1: Approve Exercise Draft

        Admin->>+APIGateway: POST /api/ai/drafts/{taskId}/approve-exercise<br/>Authorization: Bearer {admin_token}
        APIGateway->>APIGateway: Validate JWT<br/>Check role: ADMIN/INSTRUCTOR
        APIGateway->>+AiDraftController: approveExerciseDraft(taskId)
        AiDraftController->>+AiDraftApprovalService: approveExerciseDraft(taskId)

        %% Find and Validate
        rect rgb(255, 248, 220)
            Note over AiDraftApprovalService,Database: Phase 1: Find & Validate

            AiDraftApprovalService->>+AiTaskRepository: findById(taskId)
            AiTaskRepository->>Database: SELECT * FROM ai_generation_tasks<br/>WHERE id = ?
            Database-->>AiTaskRepository: AiGenerationTask
            AiTaskRepository-->>-AiDraftApprovalService: Optional<AiGenerationTask>

            alt Task not found
                AiDraftApprovalService-->>AiDraftController: throw RuntimeException
                AiDraftController-->>APIGateway: 500 "Draft not found"
                APIGateway-->>Admin: Error response
            end

            AiDraftApprovalService->>AiDraftApprovalService: Validate taskType == EXERCISE_GENERATION

            alt Wrong task type
                AiDraftApprovalService-->>AiDraftController: throw RuntimeException
                AiDraftController-->>APIGateway: 500 "Task is not exercise generation"
                APIGateway-->>Admin: Error response
            end

            AiDraftApprovalService->>AiDraftApprovalService: Validate status == DRAFT

            alt Not in DRAFT status
                AiDraftApprovalService-->>AiDraftController: throw RuntimeException
                AiDraftController-->>APIGateway: 500 "Task is not in DRAFT status"
                APIGateway-->>Admin: Error response
            end
        end

        %% Update Status
        rect rgb(220, 255, 220)
            Note over AiDraftApprovalService,Database: Phase 2: Approve

            AiDraftApprovalService->>AiDraftApprovalService: task.setStatus(APPROVED)

            AiDraftApprovalService->>+AiTaskRepository: save(task)
            AiTaskRepository->>Database: UPDATE ai_generation_tasks<br/>SET status = 'APPROVED'<br/>WHERE id = ?
            Database-->>AiTaskRepository: Updated
            AiTaskRepository-->>-AiDraftApprovalService: AiGenerationTask

            AiDraftApprovalService->>AiDraftApprovalService: Parse lessonId from targetReference
        end

        AiDraftApprovalService->>AiDraftApprovalService: Build response<br/>- taskId, lessonId, success<br/>- message: "Ready for Course Service"

        AiDraftApprovalService-->>-AiDraftController: ApproveExerciseDraftResponse
        AiDraftController-->>-APIGateway: GlobalResponse<br/>status: "DRAFT_APPROVED"
        APIGateway-->>-Admin: 200 OK<br/>{ taskId, lessonId, success: true,<br/>message: "Draft approved..." }

        Note over Admin: Admin now uses resultPayload<br/>to create exercises via<br/>PUT /api/courses/{courseId}/lessons/{lessonId}/exercise
    end

    rect rgb(255, 230, 230)
        Note over Admin,Database: 3.2: Reject Draft

        Admin->>+APIGateway: POST /api/ai/drafts/{taskId}/reject<br/>?reason=Questions are too easy
        APIGateway->>+AiDraftController: rejectDraft(taskId, reason)
        AiDraftController->>+AiDraftApprovalService: rejectDraft(taskId, reason)

        AiDraftApprovalService->>+AiTaskRepository: findById(taskId)
        AiTaskRepository-->>-AiDraftApprovalService: AiGenerationTask

        AiDraftApprovalService->>AiDraftApprovalService: Validate status == DRAFT

        AiDraftApprovalService->>AiDraftApprovalService: task.setStatus(REJECTED)<br/>task.setErrorMessage("Rejected: " + reason)

        AiDraftApprovalService->>+AiTaskRepository: save(task)
        AiTaskRepository->>Database: UPDATE ai_generation_tasks<br/>SET status = 'REJECTED',<br/>error_message = ?<br/>WHERE id = ?
        Database-->>AiTaskRepository: Updated
        AiTaskRepository-->>-AiDraftApprovalService: void

        AiDraftApprovalService-->>-AiDraftController: void
        AiDraftController-->>-APIGateway: GlobalResponse<Void><br/>status: "DRAFT_REJECTED"
        APIGateway-->>-Admin: 200 OK<br/>"Draft rejected successfully"
    end
```

### 6.4 Complete Flow: Generate → Review → Approve → Create Exercise

```mermaid
sequenceDiagram
    autonumber
    participant Instructor
    participant Admin
    participant APIGateway
    participant AIService as AI Service
    participant CourseService as Course Service
    participant Qdrant
    participant OpenAI

    rect rgb(230, 245, 255)
        Note over Instructor,OpenAI: Complete Exercise Generation Workflow

        %% Step 1: Instructor generates
        Instructor->>+APIGateway: POST /api/ai/exercises/generate<br/>{ lessonId, type: "MCQ", count: 5 }
        APIGateway->>+AIService: Forward request

        AIService->>+Qdrant: Get lesson content
        Qdrant-->>-AIService: { title, content }

        AIService->>+OpenAI: Generate exercises
        OpenAI-->>-AIService: { exercises: [...] }

        AIService->>AIService: Save as DRAFT
        AIService-->>-APIGateway: { taskId, status: "DRAFT" }
        APIGateway-->>-Instructor: 200 OK

        Note over Instructor: Notifies Admin to review

        %% Step 2: Admin reviews
        Admin->>+APIGateway: GET /api/ai/drafts/exercises?lessonId={uuid}
        APIGateway->>+AIService: Get drafts
        AIService-->>-APIGateway: List of drafts
        APIGateway-->>-Admin: Show draft exercises

        Admin->>Admin: Reviews exercises<br/>Checks quality

        %% Step 3: Admin approves
        Admin->>+APIGateway: POST /api/ai/drafts/{taskId}/approve-exercise
        APIGateway->>+AIService: Approve
        AIService->>AIService: status = APPROVED
        AIService-->>-APIGateway: { taskId, lessonId, success }
        APIGateway-->>-Admin: Draft approved

        %% Step 4: Create exercise in Course Service
        Admin->>+APIGateway: PUT /api/courses/{courseId}/lessons/{lessonId}/exercise<br/>{ exercises from resultPayload }
        APIGateway->>+CourseService: Create exercises
        CourseService->>CourseService: Save exercises to DB
        CourseService-->>-APIGateway: { exerciseId }
        APIGateway-->>-Admin: Exercises created!

        Note over Admin: Exercises now available<br/>for learners
    end
```

---

## 7. State Diagrams

### 7.1 AI Task Status Flow

```mermaid
stateDiagram-v2
    [*] --> DRAFT: AI generates exercises

    DRAFT --> APPROVED: Admin approves
    DRAFT --> REJECTED: Admin rejects

    APPROVED --> [*]: Ready for Course Service
    REJECTED --> [*]: Discarded

    note right of DRAFT
        Waiting for admin review
        Contains resultPayload with exercises
    end note

    note right of APPROVED
        Admin confirmed quality
        resultPayload ready to use
    end note

    note right of REJECTED
        Quality not acceptable
        Instructor can regenerate
    end note
```

### 7.2 Exercise Generation Flow

```mermaid
flowchart TD
    A[Instructor Request] --> B{Lesson in Qdrant?}
    B -->|No| C[❌ Error: Reindex needed]
    B -->|Yes| D[Fetch lesson content]

    D --> E[Build AI prompt]
    E --> F[Call OpenAI GPT]

    F --> G{AI Response OK?}
    G -->|No| H[❌ Task FAILED]
    G -->|Yes| I[Save as DRAFT]

    I --> J[Return taskId to Instructor]
    J --> K[Admin Reviews Draft]

    K --> L{Quality OK?}
    L -->|No| M[Reject with reason]
    L -->|Yes| N[Approve draft]

    M --> O[Status: REJECTED]
    N --> P[Status: APPROVED]

    P --> Q[Create Exercise in Course Service]
    Q --> R[✅ Exercises live!]

    O --> S[Instructor can regenerate]
    S --> A
```

---

## 8. Error Handling

| Error Case                   | HTTP Status | Message                                        |
| ---------------------------- | ----------- | ---------------------------------------------- |
| User not authenticated       | 401         | Unauthorized                                   |
| User not INSTRUCTOR/ADMIN    | 403         | Forbidden - Insufficient role                  |
| Lesson not found in Qdrant   | 500         | Lesson content not found in AI index           |
| OpenAI API error             | 500         | AI generation failed: {error}                  |
| Draft not found              | 500         | Draft not found: {taskId}                      |
| Task not EXERCISE_GENERATION | 500         | Task is not an exercise generation task        |
| Task not in DRAFT status     | 500         | Task is not in DRAFT status, current: {status} |
| Invalid request (validation) | 400         | Validation error details                       |
| Database error               | 500         | Internal server error                          |

---

## 9. Business Rules

### 9.1 Exercise Types

| Type   | Description                     | Required Fields                  |
| ------ | ------------------------------- | -------------------------------- |
| MCQ    | Multiple Choice Question        | question, options, correctAnswer |
| CODING | Coding exercise with test cases | question, testCases, explanation |
| ESSAY  | Open-ended essay question       | question, explanation (sample)   |

### 9.2 Difficulty Levels

| Level        | Description                            |
| ------------ | -------------------------------------- |
| BEGINNER     | Basic concepts, simple questions       |
| INTERMEDIATE | Applied knowledge, moderate complexity |
| ADVANCED     | Complex scenarios, edge cases          |

### 9.3 Draft Management

1. **One Lesson, Many Drafts**:

   - Mỗi lesson có thể có nhiều drafts
   - Instructor có thể generate nhiều lần với different settings
   - Admin chọn draft tốt nhất để approve

2. **Draft Lifecycle**:

   - Draft tạo xong luôn có status = DRAFT
   - Chỉ Admin có thể approve/reject
   - Approved draft → dùng resultPayload tạo exercise
   - Rejected draft → instructor có thể regenerate

3. **Target Reference**:
   - `targetReference` = lessonId (UUID string)
   - Dùng để query tất cả drafts của lesson

### 9.4 AI Prompt Structure

```
Generate {count} {type} exercises for the lesson '{title}'.

Lesson Content:
{content}

Difficulty: {difficulty}

Requirements:
1. Return a JSON object with key 'exercises' containing an array.
2. Each exercise must have: type (MCQ/CODING/ESSAY), question, options (for MCQ), testCases (for CODING), explanation.
3. For MCQ: options is array of strings.
4. For CODING: testCases is array of {input, expectedOutput}.
5. All questions must relate directly to the lesson content.
```

### 9.5 Security

1. **Role-Based Access**:

   - Generate: INSTRUCTOR, ADMIN
   - Review/Approve/Reject: ADMIN (hoặc course owner INSTRUCTOR)

2. **Lesson Verification**:
   - Lesson phải tồn tại trong Qdrant (đã indexed)
   - CourseId phải match với lesson

### 9.6 Integration with Course Service

Sau khi draft được approve:

1. Admin lấy `resultPayload` (exercises array)
2. Call Course Service API: `PUT /api/courses/{courseId}/lessons/{lessonId}/exercise`
3. Course Service tạo Exercise entities từ payload

---

## Tóm tắt các thành phần

| Component                    | Service        | Vai trò                               |
| ---------------------------- | -------------- | ------------------------------------- |
| `AiExerciseController`       | ai-service     | Endpoint generate exercises           |
| `AiDraftController`          | ai-service     | Endpoint quản lý drafts               |
| `AiExerciseService`          | ai-service     | Business logic generate               |
| `AiDraftApprovalService`     | ai-service     | Business logic approve/reject         |
| `VectorService`              | ai-service     | Fetch lesson từ Qdrant                |
| `OpenAiGateway`              | ai-service     | Call OpenAI API                       |
| `AiGenerationTaskRepository` | ai-service     | CRUD AI tasks                         |
| `QdrantClient`               | ai-service     | Vector database client                |
| `ExerciseController`         | course-service | CRUD exercises                        |
| `ExerciseService`            | course-service | Business logic exercises              |
| `OpenAI API`                 | External       | AI text generation                    |
| `Qdrant`                     | External       | Vector database lưu lesson embeddings |

---

## Configuration

```yaml
# OpenAI Configuration
openai:
  api-key: ${OPENAI_API_KEY}
  model: gpt-4
  max-tokens: 4000
  temperature: 0.7

# Qdrant Configuration
qdrant:
  host: localhost
  port: 6333
  api-key: ${QDRANT_API_KEY}
  lesson-collection: lessons
```
